package com.requirements.backend.service;



import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.List;

import java.util.Locale;



import org.springframework.stereotype.Service;



import com.requirements.backend.model.Interview;

import com.requirements.backend.model.Requirement;

import com.requirements.backend.model.RequirementUnderstandingState;

import com.requirements.backend.model.SRS;

import com.requirements.backend.repository.InterviewRepository;

import com.requirements.backend.repository.RequirementRepository;

import com.requirements.backend.repository.SRSRepository;



@Service

public class SRSService {



   private final SRSRepository srsRepository;

   private final RequirementRepository requirementRepository;

   private final InterviewRepository interviewRepository;



   public SRSService(

           SRSRepository srsRepository,

           RequirementRepository requirementRepository,

           InterviewRepository interviewRepository) {



       this.srsRepository = srsRepository;

       this.requirementRepository = requirementRepository;

       this.interviewRepository = interviewRepository;

   }



   // =====================================================

   // Generate Structured SRS

   // =====================================================



   public SRS generateSRS(String interviewId) {



       Interview interview =

               interviewRepository.findById(interviewId)

                       .orElseThrow(() ->

                               new IllegalStateException(

                                       "Interview not found: " + interviewId

                               )

                       );



       List<Requirement> requirements =

               requirementRepository.findByInterviewId(interviewId);



       if (requirements.isEmpty()) {

           throw new IllegalStateException(

                   "No requirements found for interview: " + interviewId

           );

       }



       // =================================================

       // Generate SRS only from validated requirements

       // =================================================



       for (Requirement requirement : requirements) {



           if (!"VALIDATED".equalsIgnoreCase(

                   requirement.getStatus())) {



               throw new IllegalStateException(

                       "All requirements must be validated "

                               + "before generating the SRS."

               );

           }

       }



       String projectName =

               interview.getProjectName() != null

                       ? interview.getProjectName()

                       : requirements.get(0).getProjectName();



       String stakeholderName =

               interview.getStakeholderName();



       List<String> functionalRequirements =

               new ArrayList<>();



       List<String> nonFunctionalRequirements =

               new ArrayList<>();



       List<String> userRoleRequirements =

               new ArrayList<>();



       List<String> dataRequirements =

               new ArrayList<>();



       List<String> securityRequirements =

               new ArrayList<>();



       List<String> performanceRequirements =

               new ArrayList<>();



       List<String> notificationRequirements =

               new ArrayList<>();



       List<String> errorHandlingRequirements =

               new ArrayList<>();



       // =================================================

       // Classify validated requirements

       // =================================================



       for (Requirement requirement : requirements) {



           String text = requirement.getText();



           if (text == null || text.isBlank()) {

               continue;

           }



           String normalized =

                   text.toLowerCase(Locale.ROOT);



           // =============================================

           // Primary FR / NFR classification

           // =============================================



           if ("FUNCTIONAL".equalsIgnoreCase(

                   requirement.getType())) {



               addIfAbsent(

                       functionalRequirements,

                       text

               );



           } else if ("NON_FUNCTIONAL".equalsIgnoreCase(

                   requirement.getType())) {



               addIfAbsent(

                       nonFunctionalRequirements,

                       text

               );

           }



           // =============================================

           // User Role / Authorization Requirements

           //

           // Do NOT classify every requirement mentioning

           // a customer/user as a role requirement.

           // =============================================



           boolean explicitRoleBehavior =

                   containsAny(

                           normalized,

                           "role-based",

                           "access control",

                           "permitted for their roles",

                           "permitted for the role",

                           "manage users",

                           "manage system access"

                   )

                   || startsWithAny(

                           normalized,

                           "restaurant staff ",

                           "administrators ",

                           "administrator ",

                           "librarians ",

                           "librarian ",

                           "doctors ",

                           "doctor ",

                           "staff "

                   );



           if (explicitRoleBehavior) {



               addIfAbsent(

                       userRoleRequirements,

                       text

               );

           }



           // =============================================

           // Data Requirements

           //

           // Require explicit storage / data-management

           // language. Generic "information" is not enough.

           // =============================================



           boolean explicitDataRequirement =

                   containsAny(

                           normalized,

                           "system should store",

                           "system shall store",

                           "store customer",

                           "store patient",

                           "store member",

                           "store order",

                           "store book",

                           "store data",

                           "stored data",

                           "database",

                           "maintain records",

                           "maintain customer records",

                           "maintain patient records",

                           "maintain member records",

                           "borrowing records",

                           "payment transaction details"

                   );



           if (explicitDataRequirement) {



               addIfAbsent(

                       dataRequirements,

                       text

               );

           }



           // =============================================

           // Security Requirements

           // =============================================



           boolean securityRequirement =

                   containsAny(

                           normalized,

                           "password",

                           "securely hashed",

                           "authentication",

                           "authenticate",

                           "authorization",

                           "authorized",

                           "access control",

                           "role-based",

                           "role based access",

                           "permission"

                   )

                   || isExplicitLoginRequirement(normalized);



           if (securityRequirement) {



               addIfAbsent(

                       securityRequirements,

                       text

               );

           }



           // =============================================

           // Performance Requirements

           // =============================================



           boolean performanceRequirement =

                   containsAny(

                           normalized,

                           "response time",

                           "load within",

                           "loads within",

                           "page should load",

                           "pages should load",

                           "milliseconds",

                           "latency",

                           "concurrent users",

                           "scalability",

                           "scalable"

                   )

                   || containsMeasuredSeconds(normalized);



           if (performanceRequirement) {



               addIfAbsent(

                       performanceRequirements,

                       text

               );

           }



           // =============================================

           // Notification Requirements

           //

           // "email" alone is intentionally NOT enough.

           // This prevents login-email requirements from

           // becoming notification requirements.

           // =============================================



           boolean notificationRequirement =

                   containsAny(

                           normalized,

                           "notification",

                           "notifications",

                           "notify ",

                           "notify the",

                           "receive an email",

                           "receive email",

                           "send an email",

                           "send email",

                           "email notification",

                           "sms notification",

                           "send an sms",

                           "receive an sms",

                           "alert the",

                           "receive an alert",

                           "send an alert",

                           "reminder"

                   );



           if (notificationRequirement) {



               addIfAbsent(

                       notificationRequirements,

                       text

               );

           }



           // =============================================

           // Error Handling Requirements

           // =============================================



           boolean errorRequirement =

                   containsAny(

                           normalized,

                           "display a clear error",

                           "display an error",

                           "error message",

                           "invalid information",

                           "invalid input",

                           "operation fails",

                           "operation failed",

                           "operation failure",

                           "unavailable item",

                           "not available",

                           "handle errors",

                           "handle failures",

                           "prevent the operation"

                   );



           if (errorRequirement) {



               addIfAbsent(

                       errorHandlingRequirements,

                       text

               );

           }

       }



       // =================================================

       // Requirement Understanding State

       // =================================================



       RequirementUnderstandingState state =

               interview.getUnderstandingState();



       List<RequirementUnderstandingState.CoverageItem> coverage =

               new ArrayList<>();



       List<RequirementUnderstandingState.AmbiguityItem> ambiguities =

               new ArrayList<>();



       List<String> missingInformation =

               new ArrayList<>();



       if (state != null) {



           if (state.getCoverage() != null) {

               coverage.addAll(

                       state.getCoverage()

               );

           }



           if (state.getAmbiguities() != null) {

               ambiguities.addAll(

                       state.getAmbiguities()

               );

           }



           if (state.getMissingInformation() != null) {

               missingInformation.addAll(

                       state.getMissingInformation()

               );

           }

       }



       // =================================================

       // Build Structured SRS Text

       // =================================================



       String introduction =

               "This Software Requirements Specification (SRS) "

                       + "documents the validated requirements for the "

                       + projectName

                       + " project. The requirements were collected "

                       + "through the stakeholder requirement interview, "

                       + "analyzed for coverage and ambiguity, clarified "

                       + "where necessary, and validated before SRS "

                       + "generation.";



       String purpose =

               "The purpose of this document is to provide a clear "

                       + "and structured specification of the validated "

                       + "software requirements for "

                       + projectName

                       + ". It serves as a requirements reference for "

                       + "the stakeholder and the Business Analyst.";



       String scope =

               "The scope of this SRS is limited to requirements "

                       + "identified, clarified, and validated during "

                       + "the Requirements Engineering process. "

                       + "Technical details or requirements that were "

                       + "not provided and validated by the stakeholder "

                       + "are not introduced by this document.";



       String systemOverview =

               buildSystemOverview(

                       projectName,

                       functionalRequirements,

                       nonFunctionalRequirements,

                       userRoleRequirements,

                       dataRequirements

               );



       String conclusion =

               buildConclusion(

                       projectName,

                       requirements.size(),

                       functionalRequirements.size(),

                       nonFunctionalRequirements.size(),

                       coverage,

                       ambiguities,

                       missingInformation

               );



       // =================================================

       // Maintain One Current SRS Per Interview

       // =================================================



       SRS srs =

               srsRepository.findByInterviewId(interviewId)

                       .orElseGet(SRS::new);



       srs.setInterviewId(

               interviewId

       );



       srs.setProjectName(

               projectName

       );



       srs.setStakeholderName(

               stakeholderName

       );



       srs.setIntroduction(

               introduction

       );



       srs.setPurpose(

               purpose

       );



       srs.setScope(

               scope

       );



       srs.setSystemOverview(

               systemOverview

       );



       srs.setUserRoleRequirements(

               userRoleRequirements

       );



       srs.setFunctionalRequirements(

               functionalRequirements

       );



       srs.setNonFunctionalRequirements(

               nonFunctionalRequirements

       );



       srs.setDataRequirements(

               dataRequirements

       );



       srs.setSecurityRequirements(

               securityRequirements

       );



       srs.setPerformanceRequirements(

               performanceRequirements

       );



       srs.setNotificationRequirements(

               notificationRequirements

       );



       srs.setErrorHandlingRequirements(

               errorHandlingRequirements

       );



       srs.setCoverage(

               coverage

       );



       srs.setAmbiguities(

               ambiguities

       );



       srs.setMissingInformation(

               missingInformation

       );



       srs.setTotalRequirements(

               requirements.size()

       );



       srs.setFunctionalRequirementCount(

               functionalRequirements.size()

       );



       srs.setNonFunctionalRequirementCount(

               nonFunctionalRequirements.size()

       );



       srs.setValidatedRequirementCount(

               requirements.size()

       );



       srs.setConclusion(

               conclusion

       );



       // Regeneration means the updated SRS requires BA review again.

       srs.setStatus(

               "GENERATED"

       );



       if (srs.getGeneratedAt() == null) {

           srs.setGeneratedAt(

                   LocalDateTime.now()

           );

       }



       srs.setUpdatedAt(

               LocalDateTime.now()

       );



       return srsRepository.save(

               srs

       );

   }



   // =====================================================

   // Build System Overview

   // =====================================================



   private String buildSystemOverview(

           String projectName,

           List<String> functionalRequirements,

           List<String> nonFunctionalRequirements,

           List<String> userRoleRequirements,

           List<String> dataRequirements) {



       StringBuilder overview =

               new StringBuilder();



       overview.append("The ")

               .append(projectName)

               .append(

                       " is defined by the stakeholder-validated "

                               + "requirements collected during the "

                               + "requirements interview."

               );



       if (!functionalRequirements.isEmpty()) {



           overview.append(" The system contains ")

                   .append(functionalRequirements.size())

                   .append(

                           functionalRequirements.size() == 1

                                   ? " validated functional requirement"

                                   : " validated functional requirements"

                   )

                   .append(

                           " describing the required system behavior."

                   );

       }



       if (!nonFunctionalRequirements.isEmpty()) {



           overview.append(" It also contains ")

                   .append(nonFunctionalRequirements.size())

                   .append(

                           nonFunctionalRequirements.size() == 1

                                   ? " validated non-functional requirement"

                                   : " validated non-functional requirements"

                   )

                   .append(

                           " describing quality attributes or constraints."

                   );

       }



       if (!userRoleRequirements.isEmpty()) {



           overview.append(

                   " Role-related behavior and access responsibilities "

                           + "were identified in the validated "

                           + "requirement set."

           );

       }



       if (!dataRequirements.isEmpty()) {



           overview.append(

                   " Explicit data storage or data-management "

                           + "requirements were also identified."

           );

       }



       return overview.toString();

   }



   // =====================================================

   // Build Conclusion

   // =====================================================



   private String buildConclusion(

           String projectName,

           int totalRequirements,

           int functionalCount,

           int nonFunctionalCount,

           List<RequirementUnderstandingState.CoverageItem> coverage,

           List<RequirementUnderstandingState.AmbiguityItem> ambiguities,

           List<String> missingInformation) {



       long coveredAreas =

               coverage.stream()

                       .filter(item ->

                               item != null

                                       && "COVERED".equalsIgnoreCase(

                                               item.getStatus()

                                       )

                       )

                       .count();



       StringBuilder conclusion =

               new StringBuilder();



       conclusion.append(

                       "This SRS represents the finalized and validated "

                               + "requirement set for "

               )

               .append(projectName)

               .append(". It contains ")

               .append(totalRequirements)

               .append(" validated requirements, including ")

               .append(functionalCount)

               .append(" functional and ")

               .append(nonFunctionalCount)

               .append(" non-functional requirements.");



       if (!coverage.isEmpty()) {



           conclusion.append(" ")

                   .append(coveredAreas)

                   .append(" of ")

                   .append(coverage.size())

                   .append(

                           " tracked requirement coverage areas "

                                   + "are marked as covered."

                   );

       }



       conclusion.append(" Remaining ambiguities: ")

               .append(ambiguities.size())

               .append(". Missing information items: ")

               .append(missingInformation.size())

               .append(

                       ". The document is ready for "

                               + "Business Analyst review."

               );



       return conclusion.toString();

   }



   // =====================================================

   // General Phrase Matching

   // =====================================================



    // Match a dedicated login requirement, rather than a list of user features.
    private boolean isExplicitLoginRequirement(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.matches(
                ".*\\b(?:log in|login|sign in|sign-in)\\s+(?:using|with)\\s+"
                        + "(?:their\\s+)?(?:registered\\s+)?"
                        + "(?:email|username|user name|credentials)\\b.*"
        );
    }

   private boolean containsAny(

           String text,

           String... phrases) {



       if (text == null) {

           return false;

       }



       for (String phrase : phrases) {



           if (text.contains(

                   phrase.toLowerCase(Locale.ROOT)

           )) {

               return true;

           }

       }



       return false;

   }



   // =====================================================

   // Beginning-of-Requirement Matching

   // =====================================================



   private boolean startsWithAny(

           String text,

           String... phrases) {



       if (text == null) {

           return false;

       }



       String normalized =

               text.trim().toLowerCase(Locale.ROOT);



       for (String phrase : phrases) {



           if (normalized.startsWith(

                   phrase.toLowerCase(Locale.ROOT)

           )) {

               return true;

           }

       }



       return false;

   }



   // =====================================================

   // Performance Measurement Detection

   // =====================================================



   private boolean containsMeasuredSeconds(

           String text) {



       if (text == null) {

           return false;

       }



       String normalized =

               text.toLowerCase(Locale.ROOT);



       boolean containsTimeUnit =

               normalized.contains(" second")

                       || normalized.contains(" seconds");



       boolean containsPerformanceContext =

               normalized.contains("load")

                       || normalized.contains("response")

                       || normalized.contains("page")

                       || normalized.contains("request");



       return containsTimeUnit

               && containsPerformanceContext;

   }



   // =====================================================

   // Avoid Duplicate Entries

   // =====================================================



   private void addIfAbsent(

           List<String> target,

           String value) {



       if (value == null || value.isBlank()) {

           return;

       }



       for (String existing : target) {



           if (existing.equalsIgnoreCase(value)) {

               return;

           }

       }



       target.add(value);

   }



   // =====================================================

   // Get SRS by Interview ID

   // =====================================================



   public SRS getSRSByInterviewId(

           String interviewId) {



       return srsRepository.findByInterviewId(interviewId)

               .orElseThrow(() ->

                       new RuntimeException(

                               "SRS not found for interview: "

                                       + interviewId

                       )

               );

   }



   // =====================================================

   // Business Analyst Reviews SRS

   // =====================================================



   public SRS reviewSRS(

           String interviewId) {



       SRS srs =

               srsRepository.findByInterviewId(interviewId)

                       .orElseThrow(() ->

                               new RuntimeException(

                                       "SRS not found for interview: "

                                               + interviewId

                               )

                       );



       srs.setStatus(

               "REVIEWED"

       );



       srs.setUpdatedAt(

               LocalDateTime.now()

       );



       return srsRepository.save(

               srs

       );

   }

}