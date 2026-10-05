const API_BASE = import.meta.env.VITE_API_BASE_URL
  ? `${import.meta.env.VITE_API_BASE_URL}/api`
  : '/api'

async function request(url, options = {}) {
  const response = await fetch(`${API_BASE}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  })

  if (!response.ok) {
    let message = 'Something went wrong.'

    try {
      const error = await response.json()
      message = error.message || error.error || message
    } catch {
      // Keep default message if the response is not JSON.
    }

    throw new Error(message)
  }

  if (response.status === 204) {
    return null
  }

  const responseText = await response.text()

  if (!responseText.trim()) {
    return null
  }

  return JSON.parse(responseText)
}

// ======================================================
// Authentication
// ======================================================

export function registerUser(
  name,
  email,
  password,
  role,
  assignmentCode = null
) {
  return request('/auth/register', {
    method: 'POST',
    body: JSON.stringify({
      name,
      email,
      password,
      role,
      assignmentCode,
    }),
  })
}

export function loginUser(email, password) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({
      email,
      password,
    }),
  })
}

// ======================================================
// Users / Business Analysts
// ======================================================

export function getBusinessAnalysts() {
  return request('/users/business-analysts')
}

// ======================================================
// Interviews
// ======================================================

export function getInterviews() {
  return request('/interviews')
}

// Get only interviews belonging to one Stakeholder
export function getInterviewsForStakeholder(userId) {
  return request(`/interviews/stakeholder/${userId}`)
}

// Get only interviews assigned to one Business Analyst
export function getInterviewsForBusinessAnalyst(userId) {
  return request(`/interviews/business-analyst/${userId}`)
}

export function getInterview(interviewId) {
  return request(`/interviews/${interviewId}`)
}

export function createInterview(
  projectName,
  stakeholderName,
  stakeholderUserId,
  businessAnalystUserId
) {
  return request('/interviews', {
    method: 'POST',
    body: JSON.stringify({
      projectName,
      stakeholderName,
      stakeholderUserId,
      businessAnalystUserId,
    }),
  })
}

export function sendStakeholderResponse(interviewId, response) {
  return request(`/interviews/${interviewId}/respond`, {
    method: 'POST',
    body: JSON.stringify({
      response,
    }),
  })
}

// ======================================================
// Requirements
// ======================================================

export function getRequirements(interviewId) {
  return request(`/requirements/interview/${interviewId}`)
}

export function addRequirement(interviewId, text, type) {
  return request(`/requirements/interview/${interviewId}`, {
    method: 'POST',
    body: JSON.stringify({
      text,
      type,
    }),
  })
}

export function validateRequirement(requirementId) {
  return request(`/requirements/${requirementId}/validate`, {
    method: 'PATCH',
  })
}

export function correctRequirement(requirementId, text, type) {
  return request(`/requirements/${requirementId}/correct`, {
    method: 'PATCH',
    body: JSON.stringify({
      text,
      type,
    }),
  })
}

export function deleteRequirement(requirementId) {
  return request(`/requirements/${requirementId}`, {
    method: 'DELETE',
  })
}

export function finalizeRequirements(interviewId) {
  return request(`/requirements/interview/${interviewId}/finalize`, {
    method: 'PATCH',
  })
}

// ======================================================
// SRS
// ======================================================

export function generateSRS(interviewId) {
  return request(`/srs/interview/${interviewId}/generate`, {
    method: 'POST',
  })
}

export function getSRS(interviewId) {
  return request(`/srs/interview/${interviewId}`)
}

export function reviewSRS(interviewId) {
  return request(`/srs/interview/${interviewId}/review`, {
    method: 'PATCH',
  })
}