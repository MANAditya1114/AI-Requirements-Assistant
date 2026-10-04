import json
import re
import socket
import urllib.request
import urllib.error
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
# ==========================================================
# FastAPI Application
# ==========================================================
app = FastAPI(
    title="AI Requirements Engineering Assistant",
    version="1.0"
)
# ==========================================================
# Input Model
# ==========================================================


class StakeholderResponse(BaseModel):
    project_name: str
    response: str
# ==========================================================
# Coverage Configuration
# ==========================================================
COVERAGE_AREAS = [
    "User Roles",
    "Core Features",
    "Data Requirements",
    "Security",
    "Performance",
    "Notifications",
    "Error Handling"
]
VALID_COVERAGE_STATUSES = [
    "COVERED",
    "PARTIAL",
    "NOT_DISCOVERED"
]
# ==========================================================
# Health Check
# ==========================================================
@app.get("/")
def home():
    return {
        "status": "running",
        "service": "AI Requirements Engineering Assistant",
        "llm": "Qwen3 8B",
        "thinking": "disabled"
    }
# ==========================================================
# General Helper Functions
# ==========================================================


def normalize_text(text: str) -> str:
    return re.sub(
        r"\s+",
        " ",
        text.lower()
    ).strip()


def contains_any(
    text: str,
    keywords: list[str]
) -> bool:
    return any(
        keyword in text
        for keyword in keywords
    )
# ==========================================================
# Performance Requirement Helpers
# ==========================================================


def has_measurable_performance_target(
    text: str
) -> bool:
    """

    Detect whether the stakeholder supplied a measurable

    performance target.

    """
    patterns = [
        r"\bwithin\s+\d+(?:\.\d+)?\s*(?:ms|millisecond|milliseconds|second|seconds|sec|secs|minute|minutes)\b",
        r"\bunder\s+\d+(?:\.\d+)?\s*(?:ms|millisecond|milliseconds|second|seconds|sec|secs|minute|minutes)\b",
        r"\bless\s+than\s+\d+(?:\.\d+)?\s*(?:ms|millisecond|milliseconds|second|seconds|sec|secs|minute|minutes)\b",
        r"\b\d+(?:\.\d+)?\s*(?:ms|millisecond|milliseconds|second|seconds|sec|secs)\s+(?:response|response time|load time)\b",
        r"\b\d+(?:\.\d+)?\s*(?:requests|transactions|users)\s+per\s+(?:second|minute)\b"
    ]
    return any(
        re.search(
            pattern,
            text,
            re.IGNORECASE
        )
        for pattern in patterns
    )


def performance_was_mentioned(
    text: str
) -> bool:
    normalized = normalize_text(
        text
    )
    keywords = [
        "very fast",
        "fast",
        "quickly",
        "performance",
        "response time",
        "load time",
        "load within",
        "loads within",
        "seconds",
        "milliseconds",
        "requests per second",
        "transactions per second"
    ]
    return contains_any(
        normalized,
        keywords
    )


def remove_resolved_performance_ambiguities(
    result: dict
) -> None:
    cleaned = []
    for ambiguity in result.get(
        "ambiguities",
        []
    ):
        if not isinstance(
            ambiguity,
            dict
        ):
            continue
        text = normalize_text(
            str(
                ambiguity.get(
                    "text",
                    ""
                )
            )
        )
        reason = normalize_text(
            str(
                ambiguity.get(
                    "reason",
                    ""
                )
            )
        )
        performance_related = (
            contains_any(
                text,
                [
                    "very fast",
                    "fast",
                    "quickly",
                    "performance",
                    "response time",
                    "load time"
                ]
            )
            or
            contains_any(
                reason,
                [
                    "performance",
                    "measurable",
                    "response time",
                    "load time",
                    "target"
                ]
            )
        )
        if performance_related:
            continue
        cleaned.append(
            ambiguity
        )
    result["ambiguities"] = cleaned


def remove_resolved_performance_missing_information(
    result: dict
) -> None:
    cleaned = []
    for item in result.get(
        "missing_information",
        []
    ):
        if not isinstance(
            item,
            str
        ):
            continue
        normalized = normalize_text(
            item
        )
        performance_related = contains_any(
            normalized,
            [
                "performance",
                "response time",
                "load time",
                "very fast",
                "measurable target",
                "speed target"
            ]
        )
        if performance_related:
            continue
        cleaned.append(
            item
        )
    result["missing_information"] = cleaned


def set_coverage_status(
    result: dict,
    area_name: str,
    status_value: str
) -> None:
    for item in result.get(
        "coverage",
        []
    ):
        if (
            isinstance(
                item,
                dict
            )
            and item.get(
                "area"
            ) == area_name
        ):
            item["status"] = (
                status_value
            )
            return


def replace_vague_performance_requirement(
    result: dict
) -> None:
    """

    If a measurable performance requirement exists,

    remove an older vague requirement such as

    "The system should be very fast."

    """
    requirements = result.get(
        "requirements",
        []
    )
    has_specific_performance_requirement = False
    for requirement in requirements:
        if not isinstance(
            requirement,
            dict
        ):
            continue
        text = str(
            requirement.get(
                "text",
                ""
            )
        )
        if has_measurable_performance_target(
            text
        ):
            has_specific_performance_requirement = True
            break
    if not has_specific_performance_requirement:
        return
    cleaned = []
    vague_phrases = [
        "very fast",
        "high performance",
        "quickly"
    ]
    for requirement in requirements:
        if not isinstance(
            requirement,
            dict
        ):
            continue
        text = normalize_text(
            str(
                requirement.get(
                    "text",
                    ""
                )
            )
        )
        is_vague_performance_requirement = (
            requirement.get(
                "type"
            ) == "NON_FUNCTIONAL"
            and contains_any(
                text,
                vague_phrases
            )
            and not has_measurable_performance_target(
                text
            )
        )
        if is_vague_performance_requirement:
            continue
        cleaned.append(
            requirement
        )
    result["requirements"] = (
        cleaned
    )


def fix_performance_clarification(
    result: dict,
    stakeholder_text: str
) -> None:
    """

    Deterministically resolves a vague performance

    requirement when the stakeholder later supplies

    a measurable target.

    """
    if not performance_was_mentioned(
        stakeholder_text
    ):
        return
    if not has_measurable_performance_target(
        stakeholder_text
    ):
        return
    remove_resolved_performance_ambiguities(
        result
    )
    remove_resolved_performance_missing_information(
        result
    )
    replace_vague_performance_requirement(
        result
    )
    set_coverage_status(
        result,
        "Performance",
        "COVERED"
    )
# ==========================================================
# Coverage Helpers
# ==========================================================


def reconcile_notification_coverage(
    result: dict,
    stakeholder_text: str
) -> None:
    """Resolve notification coverage from the complete accumulated interview."""
    # Ground coverage in stakeholder notification statements, not model output.
    # Keep channel/event evidence local so login email and unrelated failures
    # elsewhere in the interview cannot satisfy a notification requirement.
    notification_pattern = (
        r"\b(?:notifications?|notify|notified|alerts?|reminders?)\b"
    )
    delivery_pattern = (
        r"\b(?:send|sent|receive|received|deliver|delivered)\b"

        r"[^.!?;]{0,80}\b(?:e-?mails?|sms|text messages?|messages?)\b"
    )
    channel_pattern = (
        r"\b(?:e-?mails?|sms|text messages?|push(?: notifications?)?|"

        r"in[- ]app)\b"
    )
    event_pattern = (
        r"\b(?:when|after|before|upon)\s+\w+|"

        r"\b(?:due|overdue|confirmations?|confirmed|"

        r"cancellations?|cancel(?:led|ed)?|completions?|completed|failures?|"

        r"failed|approvals?|approved|rejections?|rejected|created|updated|"

        r"borrowed|returned|available)\b"
    )
    has_notification = False
    has_channel = False
    has_event = False
    covered = False
    for statement in re.split(r"[.!?;\n]+", stakeholder_text):
        text = normalize_text(statement)
        if not (
            re.search(notification_pattern, text)
            or re.search(delivery_pattern, text)
        ):
            continue
        # Authentication messages alone do not establish notification intent.
        if re.search(
            r"\b(?:log[- ]?in|sign[- ]?in|authentication|verification|"

            r"verify|password|otp|one[- ]time)\b", text
        ) and not re.search(notification_pattern, text):
            continue
        if re.search(
            r"\b(?:no|without)\s+(?:\w+\s+){0,2}"

            r"(?:notifications?|alerts?|reminders?)\b|"

            r"\b(?:do not|don't|should not|must not|never)\s+"

            r"(?:\w+\s+){0,2}(?:notify|send|receive|alert|remind)\b",
            text
        ):
            continue
        has_notification = True
        statement_channel = re.search(channel_pattern, text) is not None
        statement_event = re.search(event_pattern, text) is not None
        has_channel = has_channel or statement_channel
        has_event = has_event or statement_event
        covered = covered or (statement_channel and statement_event)
    status = (
        "COVERED" if covered else
        "PARTIAL" if has_notification else
        "NOT_DISCOVERED"
    )
    set_coverage_status(result, "Notifications", status)
    if not covered:
        return
    missing_information = result.get("missing_information", [])
    if not isinstance(missing_information, list):
        return
    def asks_only_for_supplied_details(item):
        if not isinstance(item, str):
            return False
        question = normalize_text(item)
        if re.search(notification_pattern, question) is None:
            return False
        # Preserve questions about additional notification requirements.
        if re.search(
            r"\b(?:retries|retry|delivery|latency|frequency|schedule|timing|"

            r"minutes?|hours?|days?|preferences?|opt[- ]out|consent|privacy|"

            r"security|recipients?|templates?|contents?|retention|"

            r"escalation|priority|reliability|rate|limits?)\b",
            question
        ):
            return False
        return re.search(
            r"\b(?:channels?|methods?|medium|media|events?|triggers?|when|"

            r"email|sms|text|push|in[- ]app)\b",
            question
        ) is not None
    result["missing_information"] = [
        item for item in missing_information
        if not asks_only_for_supplied_details(item)
    ]


def normalize_coverage(
    result: dict
) -> None:
    returned_coverage = {}
    for item in result.get(
        "coverage",
        []
    ):
        if not isinstance(
            item,
            dict
        ):
            continue
        area = item.get(
            "area"
        )
        status = item.get(
            "status"
        )
        if isinstance(
            status,
            str
        ):
            status = (
                status
                .strip()
                .upper()
            )
        if (
            area in COVERAGE_AREAS
            and status in VALID_COVERAGE_STATUSES
        ):
            returned_coverage[
                area
            ] = status
    normalized_coverage = []
    for area in COVERAGE_AREAS:
        status = returned_coverage.get(
            area,
            "NOT_DISCOVERED"
        )
        normalized_coverage.append({
            "area": area,
            "status": status
        })
    result["coverage"] = (
        normalized_coverage
    )
# ==========================================================
# Adaptive Question Helper
# ==========================================================


def reconcile_covered_missing_information(result: dict) -> None:
    """Clear required gaps when every coverage area is complete.

    Optional implementation details do not keep the interview incomplete.
    Actual required gaps must remain PARTIAL or NOT_DISCOVERED, and
    outstanding ambiguities prevent completion.
    """
    coverage = result.get("coverage", [])
    if not isinstance(coverage, list):
        return
    statuses = {}
    for entry in coverage:
        if not isinstance(entry, dict):
            continue
        area = entry.get("area")
        if isinstance(area, str) and area in COVERAGE_AREAS:
            statuses.setdefault(area, []).append(entry.get("status"))
    if not all(
        statuses.get(area) == ["COVERED"] for area in COVERAGE_AREAS
    ):
        return
    # Completion alone cannot resolve an outstanding ambiguity.
    if result.get("ambiguities") != []:
        return
    result["missing_information"] = []
    return


def choose_next_question(
    result: dict
) -> str:
    """

    Select the next requirements-engineering question from

    the current coverage state.

    IMPORTANT:

    These questions are intentionally domain-neutral.

    They must work for library, healthcare, banking,

    e-commerce, education, or any other software project.

    """
    coverage = {
        item.get("area"):
            item.get("status")
        for item in result.get(
            "coverage",
            []
        )
        if isinstance(
            item,
            dict
        )
    }
    question_map = {
        "User Roles":
            "What types of users will use the system and what should each user be allowed to do?",
        "Core Features":
            "What other main functions should the system provide?",
        "Data Requirements":
            "What information should the system store and manage?",
        "Security":
            "How should users authenticate, and what security or access controls are required?",
        "Performance":
            "What measurable response time or performance target should the system meet?",
        "Notifications":
            "Should the system send notifications, and when should users receive them?",
        "Error Handling":
            "How should the system handle invalid information, failed operations, or other errors?"
    }
    priority_order = [
        "User Roles",
        "Core Features",
        "Data Requirements",
        "Security",
        "Performance",
        "Notifications",
        "Error Handling"
    ]
    # First resolve PARTIAL areas.
    for area in priority_order:
        if coverage.get(
            area
        ) == "PARTIAL":
            return question_map[
                area
            ]
    # Then discover untouched areas.
    for area in priority_order:
        if coverage.get(
            area
        ) == "NOT_DISCOVERED":
            return question_map[
                area
            ]
    return (
        "Are there any additional requirements or "

        "constraints that should be included?"
    )
# ==========================================================
# Requirement Validation Helper
# ==========================================================


def clean_requirements(
    result: dict
) -> None:
    """

    Validate the requirements returned by Qwen.

    Requirement type is normalized to uppercase so a valid

    requirement is not accidentally removed only because the

    model returned 'functional' instead of 'FUNCTIONAL'.

    """
    cleaned_requirements = []
    for requirement in result.get(
        "requirements",
        []
    ):
        if not isinstance(
            requirement,
            dict
        ):
            continue
        text = requirement.get(
            "text"
        )
        requirement_type = (
            requirement.get(
                "type"
            )
        )
        if not isinstance(
            text,
            str
        ):
            continue
        text = text.strip()
        if not text:
            continue
        if isinstance(
            requirement_type,
            str
        ):
            requirement_type = (
                requirement_type
                .strip()
                .upper()
            )
        if requirement_type not in [
            "FUNCTIONAL",
            "NON_FUNCTIONAL"
        ]:
            continue
        cleaned_requirements.append({
            "text": text,
            "type": requirement_type
        })
    result["requirements"] = (
        cleaned_requirements
    )


def ensure_requirements_exist(
    result: dict,
    stakeholder_text: str
) -> None:
    """

    Safety guard.

    The /analyze endpoint receives the complete accumulated

    interview. If that interview contains stakeholder text

    but the model returns zero requirements, the result must

    not be sent to Spring Boot.

    Otherwise RequirementService could interpret the empty

    result as meaning that previously discovered requirements

    no longer exist.

    """
    if not isinstance(
        stakeholder_text,
        str
    ):
        return
    if not stakeholder_text.strip():
        return
    requirements = result.get(
        "requirements",
        []
    )
    if (
        not isinstance(
            requirements,
            list
        )
        or len(requirements) == 0
    ):
        raise HTTPException(
            status_code=502,
            detail=(
                "AI analysis returned no requirements "

                "for a non-empty accumulated interview. "

                "The analysis was rejected to protect "

                "the existing requirement state."
            )
        )
# ==========================================================
# Ambiguity Validation Helper
# ==========================================================


def clean_ambiguities(
    result: dict
) -> None:
    cleaned_ambiguities = []
    for ambiguity in result.get(
        "ambiguities",
        []
    ):
        if not isinstance(
            ambiguity,
            dict
        ):
            continue
        text = ambiguity.get(
            "text"
        )
        reason = ambiguity.get(
            "reason"
        )
        if (
            isinstance(
                text,
                str
            )
            and isinstance(
                reason,
                str
            )
            and text.strip()
            and reason.strip()
        ):
            cleaned_ambiguities.append({
                "text":
                    text.strip(),
                "reason":
                    reason.strip()
            })
    result["ambiguities"] = (
        cleaned_ambiguities
    )
# ==========================================================
# Missing Information Validation Helper
# ==========================================================


def clean_missing_information(
    result: dict
) -> None:
    cleaned_missing_information = []
    for item in result.get(
        "missing_information",
        []
    ):
        if (
            isinstance(
                item,
                str
            )
            and item.strip()
        ):
            cleaned_missing_information.append(
                item.strip()
            )
    result["missing_information"] = (
        cleaned_missing_information
    )
# ==========================================================
# Analyze Stakeholder Requirements
# ==========================================================
@app.post("/analyze")
def analyze_requirement(
    data: StakeholderResponse
):
    # ======================================================
    # Prompt
    # ======================================================
    prompt = f"""

/no_think

You are an AI Requirements Engineering Assistant.

Your task is to analyze an ACCUMULATED multi-turn

requirements interview for the software project identified

below.

PROJECT NAME:

{data.project_name}

COMPLETE ACCUMULATED STAKEHOLDER INTERVIEW:

{data.response}

CRITICAL PROJECT CONTEXT RULES

The PROJECT NAME and stakeholder interview define the current

project context.

Stay strictly within the domain of the current project.

Do NOT assume that the project belongs to healthcare,

library management, banking, e-commerce, education, or any

other domain unless that domain is supported by the current

project name or stakeholder statements.

Do NOT copy domain-specific entities, actors, features,

workflows, or terminology from examples in these

instructions.

Examples are provided only to explain requirements-engineering

logic. They are NOT requirements for the current project.

Never introduce unrelated entities such as patients,

doctors, appointments, books, customers, payments, orders,

students, or similar domain concepts unless they are

supported by the current project context.

CRITICAL ACCUMULATED INTERVIEW INSTRUCTION

The interview above contains the complete stakeholder

conversation collected so far.

You MUST extract the COMPLETE CURRENT REQUIREMENT SET from

the ENTIRE accumulated interview.

Do NOT analyze only the last stakeholder response.

Do NOT return only newly discovered requirements.

Do NOT omit older requirements merely because they appeared

in an earlier turn.

The "requirements" array must represent ALL requirements

that are currently supported by the complete accumulated

stakeholder interview.

A later response may clarify, refine, quantify, or replace

an earlier requirement.

When that happens:

- keep the latest specific meaning;

- do not keep an obsolete vague version as a separate

  requirement;

- do not remove unrelated requirements from earlier turns.

GENERIC MULTI-TURN EXAMPLE

This example demonstrates accumulation only.

Turn 1:

"The system should allow registered users to create records."

Turn 2:

"Pages should load within 2 seconds."

Turn 3:

"Users should receive an email after a successful

submission."

The final requirements array must contain ALL THREE current

requirements, not only the requirement from Turn 3.

Do NOT copy the example's users, records, submissions,

emails, or other concepts into the current project unless

the stakeholder actually supplied them.

IMPORTANT INTERVIEW BEHAVIOR

Always analyze the COMPLETE interview context.

Later responses may clarify, refine, replace, quantify,

or complete earlier requirements.

If a later statement clarifies an earlier vague statement,

use the latest specific information.

Do not keep a resolved ambiguity unresolved.

Do not invent requirements.

Do not convert missing information into a requirement.

TASKS

1. Extract ALL CURRENT software requirements from the

   complete accumulated stakeholder interview.

2. Classify every extracted requirement as exactly:

   FUNCTIONAL

   or

   NON_FUNCTIONAL

3. Detect only CURRENTLY UNRESOLVED ambiguities.

4. Identify important CURRENTLY MISSING information.

5. Evaluate coverage for exactly these seven areas:

   User Roles

   Core Features

   Data Requirements

   Security

   Performance

   Notifications

   Error Handling

6. Generate one useful follow-up question.

REQUIREMENT EXTRACTION RULES

- Read the ENTIRE accumulated interview before creating the

  requirements array.

- Extract every distinct current requirement directly

  supported by stakeholder statements.

- Requirements from earlier turns remain requirements unless

  a later statement explicitly replaces or refines them.

- A new requirement does NOT erase older unrelated

  requirements.

- Do not return only the latest requirement.

- Never invent a feature.

- Never turn missing information into a requirement.

- Functional requirements describe what the system must do.

- Non-functional requirements describe qualities or

  constraints such as performance, security, usability,

  reliability, scalability, and availability.

- Security quality constraints such as password hashing,

  encryption, secure storage, and security standards are

  NON_FUNCTIONAL.

- Authentication behavior such as allowing a registered user

  to log in is FUNCTIONAL.

- If a later requirement replaces or clarifies an earlier

  vague requirement, prefer the later specific requirement.

PERFORMANCE CLARIFICATION EXAMPLE

Earlier:

"The system should be very fast."

Later:

"Pages should load within 2 seconds."

Return:

"Pages should load within 2 seconds."

Do NOT keep:

"The system should be very fast."

as another requirement or unresolved ambiguity.

GENERIC REFINEMENT EXAMPLE

Earlier:

"Users should receive a notification when an operation is

completed."

Later:

"Completion notifications should be sent by email."

These statements may describe the same event, with the later

statement clarifying the delivery channel.

Return the refined current requirement when the later

statement clearly refines the earlier one.

If another later statement says:

"Users should also receive an email when the operation

fails."

that describes a DIFFERENT notification event.

Do not replace the successful-event notification with the

failure-event notification.

The current requirement set should preserve both distinct

events.

Do NOT copy these generic example events into the current

project unless supported by stakeholder statements.

AMBIGUITY RULES

A phrase is ambiguous only if it remains unclear after

considering ALL stakeholder responses.

Examples of potentially vague phrases include:

"very fast"

"quickly"

"easy to use"

"user-friendly"

"secure"

"efficient"

"high performance"

If later information provides sufficient clarification,

the earlier phrase is no longer an unresolved ambiguity.

MISSING INFORMATION RULES

Missing information must be relevant to the CURRENT project

and the CURRENT interview.

Do not mention domain entities that have not been introduced

by the stakeholder or clearly established by the project

name.

Describe missing information using domain-neutral language

when specific domain terminology is not known.

Do not invent missing requirements.

When all seven required coverage areas are COVERED and no unresolved
ambiguities remain, do not list optional implementation elaborations as
missing information (for example specific payment transaction fields).
Keep genuinely unresolved stakeholder requirements or constraints as missing
information. If an area still lacks required information, use PARTIAL or
NOT_DISCOVERED as appropriate; do not declare it COVERED to remove the gap.

COVERAGE STATUS RULES

Use ONLY:

COVERED

The stakeholder supplied sufficiently specific information

about that area for the current requirement interview.

PARTIAL

The stakeholder discussed the area, but the stated

requirement still needs clarification.

NOT_DISCOVERED

The stakeholder has not supplied useful information about

that area.

Do NOT mark an area COVERED merely because it could

logically be important to the project.

Coverage must be based only on information actually supplied

by the stakeholder.

Do not change an already sufficiently covered area to

PARTIAL merely because additional optional details could

also be collected.

For example, if the stakeholder explicitly specifies one

required notification channel for a particular event, do

not mark Notifications PARTIAL merely because other optional

notification channels were not discussed.

Do not ask for alternative channels unless the stakeholder

indicated that additional channels are required.

FOLLOW-UP QUESTION RULES

Ask exactly ONE question.

Ask about an unresolved PARTIAL area first.

If no useful PARTIAL area remains, ask about an important

NOT_DISCOVERED area.

Never ask again for information that has already been

provided.

Do not ask for optional alternatives merely because other

implementation choices could exist.

The question must stay within the CURRENT PROJECT context.

Do not mention domain-specific entities that were not

introduced by the project name or stakeholder.

If the domain-specific wording is uncertain, use a

domain-neutral question.

OUTPUT RULES

Return ONLY valid JSON.

Do not return Markdown.

Do not include explanations outside the JSON.

The "requirements" array MUST contain the complete current

requirement set from the entire accumulated interview.

If the interview contains software requirements, do NOT

return an empty requirements array.

Keep each requirement concise and independently

understandable.

Return exactly this structure:

{{

    "requirements": [

        {{

            "text": "requirement directly supported by stakeholder",

            "type": "FUNCTIONAL"

        }}

    ],

    "ambiguities": [

        {{

            "text": "currently ambiguous phrase",

            "reason": "why clarification is required"

        }}

    ],

    "missing_information": [

        "important currently missing information"

    ],

    "coverage": [

        {{

            "area": "User Roles",

            "status": "COVERED or PARTIAL or NOT_DISCOVERED"

        }},

        {{

            "area": "Core Features",

            "status": "COVERED or PARTIAL or NOT_DISCOVERED"

        }},

        {{

            "area": "Data Requirements",

            "status": "COVERED or PARTIAL or NOT_DISCOVERED"

        }},

        {{

            "area": "Security",

            "status": "COVERED or PARTIAL or NOT_DISCOVERED"

        }},

        {{

            "area": "Performance",

            "status": "COVERED or PARTIAL or NOT_DISCOVERED"

        }},

        {{

            "area": "Notifications",

            "status": "COVERED or PARTIAL or NOT_DISCOVERED"

        }},

        {{

            "area": "Error Handling",

            "status": "COVERED or PARTIAL or NOT_DISCOVERED"

        }}

    ],

    "next_question": "one adaptive follow-up question"

}}

"""
    # ======================================================
    # Ollama Request
    # ======================================================
    ollama_request = {
        "model": "qwen3:8b",
        "prompt": prompt,
        "stream": False,
        "format": "json",
        "think": False,
        "options": {
            "temperature": 0.1,
            # Allow enough room for the complete accumulated
            # requirement set.
            "num_predict": 3072
        }
    }
    try:
        request = urllib.request.Request(
            "http://localhost:11434/api/generate",
            data=json.dumps(
                ollama_request
            ).encode(
                "utf-8"
            ),
            headers={
                "Content-Type":
                    "application/json"
            },
            method="POST"
        )
        # ==================================================
        # Send Request to Ollama
        # ==================================================
        with urllib.request.urlopen(
            request,
            timeout=300
        ) as response:
            ollama_response = json.loads(
                response.read().decode(
                    "utf-8"
                )
            )
        # ==================================================
        # Read Qwen Response
        # ==================================================
        ai_output = ollama_response.get(
            "response"
        )
        if not ai_output:
            raise HTTPException(
                status_code=502,
                detail=(
                    "Ollama returned an empty response."
                )
            )
        result = json.loads(
            ai_output
        )
        # ==================================================
        # Validate Root Result
        # ==================================================
        if not isinstance(
            result,
            dict
        ):
            raise HTTPException(
                status_code=502,
                detail=(
                    "AI analysis did not return "

                    "a valid JSON object."
                )
            )
        # ==================================================
        # Validate Main Fields
        # ==================================================
        required_fields = [
            "requirements",
            "ambiguities",
            "missing_information",
            "coverage",
            "next_question"
        ]
        for field in required_fields:
            if field not in result:
                raise HTTPException(
                    status_code=502,
                    detail=(
                        "AI response is missing "

                        "required field: "

                        f"{field}"
                    )
                )
        # ==================================================
        # Validate Requirements
        # ==================================================
        clean_requirements(
            result
        )
        # ==================================================
        # CRITICAL EMPTY-REQUIREMENT SAFETY CHECK
        # ==================================================
        ensure_requirements_exist(
            result,
            data.response
        )
        # ==================================================
        # Validate Ambiguities
        # ==================================================
        clean_ambiguities(
            result
        )
        # ==================================================
        # Validate Missing Information
        # ==================================================
        clean_missing_information(
            result
        )
        # ==================================================
        # Normalize Coverage
        # ==================================================
        normalize_coverage(
            result
        )
        # ==================================================
        # Deterministic Requirement State Reconciliation
        # ==================================================
        fix_performance_clarification(
            result,
            data.response
        )
        reconcile_notification_coverage(
            result,
            data.response
        )
        reconcile_covered_missing_information(result)
        # ==================================================
        # Re-check Requirements After Reconciliation
        # ==================================================
        ensure_requirements_exist(
            result,
            data.response
        )
        # ==================================================
        # Generate Safe Adaptive Follow-Up
        #
        # We intentionally choose the question after Qwen's
        # analysis so the application controls the coverage
        # workflow consistently.
        # ==================================================
        result["next_question"] = (
            choose_next_question(
                result
            )
        )
        # ==================================================
        # Final Response
        # ==================================================
        return result
    # ======================================================
    # Ollama Timeout
    # ======================================================
    except (
        TimeoutError,
        socket.timeout
    ):
        raise HTTPException(
            status_code=504,
            detail=(
                "The local Qwen3 8B model took longer "

                "than 300 seconds to respond."
            )
        )
    # ======================================================
    # Ollama HTTP Error
    # ======================================================
    except urllib.error.HTTPError as e:
        try:
            error_body = (
                e.read()
                .decode(
                    "utf-8",
                    errors="replace"
                )
            )
        except Exception:
            error_body = str(
                e
            )
        raise HTTPException(
            status_code=502,
            detail=(
                "Ollama returned HTTP "

                f"{e.code}: {error_body}"
            )
        )
    # ======================================================
    # Ollama Connection Error
    # ======================================================
    except urllib.error.URLError as e:
        if isinstance(
            getattr(
                e,
                "reason",
                None
            ),
            (
                TimeoutError,
                socket.timeout
            )
        ):
            raise HTTPException(
                status_code=504,
                detail=(
                    "The local Qwen3 8B model took longer "

                    "than 300 seconds to respond."
                )
            )
        raise HTTPException(
            status_code=503,
            detail=(
                "Cannot connect to Ollama. "

                "Make sure Ollama is running."
            )
        )
    # ======================================================
    # Invalid JSON
    # ======================================================
    except json.JSONDecodeError:
        raise HTTPException(
            status_code=502,
            detail=(
                "The local LLM returned invalid JSON."
            )
        )
    # ======================================================
    # Existing HTTP Errors
    # ======================================================
    except HTTPException:
        raise
    # ======================================================
    # Other Errors
    # ======================================================
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=str(
                e
            )
        )
