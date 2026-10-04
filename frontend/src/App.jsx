import { Fragment, useEffect, useRef, useState } from 'react'
import {
  BrowserRouter,
  Routes,
  Route,
  Link,
  useNavigate,
  useLocation,
  Navigate,
  useParams,
} from 'react-router-dom'
import {
  loginUser,
  registerUser,
  createInterview,
  getInterview,
  getInterviewsForStakeholder,
  getInterviewsForBusinessAnalyst,
  sendStakeholderResponse,
  getRequirements,
  addRequirement,
  validateRequirement,
  correctRequirement,
  deleteRequirement,
  finalizeRequirements,
  generateSRS,
  getSRS,
  reviewSRS,
} from './api'
import './App.css'
const INITIAL_QUESTION =
  'Please describe the main requirements for your system.'
const ACTIVE_INTERVIEW_KEY = 'reAssistantActiveInterviewId'
const COVERAGE_AREAS = [
  'User Roles',
  'Core Features',
  'Data Requirements',
  'Security',
  'Performance',
  'Notifications',
  'Error Handling',
]
// Review entry is remembered per account/project on this browser. The current
// API has no review-entry mutation; server metadata takes precedence when present.
const workflowKey = user => `reAssistantWorkflow:${user.role}:${user.id}`
function workflow(user, id) {
  try { return JSON.parse(localStorage.getItem(workflowKey(user)) || '{}')[id] || {} }
  catch { return {} }
}
function saveWorkflow(user, id, patch) {
  const key = workflowKey(user)
  let values = {}
  try { values = JSON.parse(localStorage.getItem(key) || '{}') || {} } catch { /* Replace malformed local workflow data. */ }
  localStorage.setItem(key, JSON.stringify({ ...values, [id]: { ...values[id], ...patch } }))
}
const projectId = item => String(item?.id || item?._id || '')
function discoveryComplete(item) {
  return COVERAGE_AREAS.every(area => item?.understandingState?.coverage?.some(
    coverage => coverage.area === area && coverage.status === 'COVERED'))
}
function isFinalized(item, user) {
  return Boolean(item?.savedSRS) || item?.requirementsFinalized === true || Boolean(item?.requirementsFinalizedAt) ||
    ['FINALIZED', 'REQUIREMENTS_FINALIZED', 'SRS_GENERATED', 'SRS_REVIEWED'].includes(String(item?.status).toUpperCase()) ||
    workflow(user, projectId(item)).finalized === true
}
function reviewEntered(item, user) {
  return isFinalized(item, user) || (discoveryComplete(item) &&
    (Boolean(item?.requirementsReviewEnteredAt) || workflow(user, projectId(item)).reviewEntered === true))
}
function missingSRS(cause) {
  return /\b404\b|no srs|srs.*(?:not found|not generated|does not exist|not available)|(?:not generated|does not exist).*srs/i.test(cause?.message || '')
}
async function projectSRS(id) {
  const document = await getSRS(id)
  if (document?.interviewId && String(document.interviewId) !== String(id)) {
    throw new Error('The server returned an SRS for a different project.')
  }
  return document
}
async function ownedProjects(user) {
  const result = user.role === 'BUSINESS_ANALYST'
    ? await getInterviewsForBusinessAnalyst(user.id) : await getInterviewsForStakeholder(user.id)
  const items = Array.isArray(result) ? result : result?.interviews
  if (!Array.isArray(items)) throw new Error('The saved interviews response was not a list.')
  return items.filter(item => projectId(item) &&
    (!item.stakeholderId || user.role !== 'STAKEHOLDER' || String(item.stakeholderId) === user.id) &&
    (!item.businessAnalystId || user.role !== 'BUSINESS_ANALYST' || String(item.businessAnalystId) === user.id))
}
async function loadProjects(user) {
  const items = await ownedProjects(user)
  return Promise.all(items.map(async item => {
    const detail = await getInterview(projectId(item))
    if (projectId(detail) !== projectId(item)) throw new Error('The server returned a different project.')
    let document = null
    try { document = await projectSRS(projectId(item)) }
    catch (cause) { if (!missingSRS(cause)) throw cause }
    return { ...item, ...detail, savedSRS: document }
  }))
}
function ProjectList({ user, kind }) {
  const [items, setItems] = useState(null)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    let cancelled = false
    setItems(null); setError('')
    loadProjects(user).then(result => { if (!cancelled) setItems(result) })
      .catch(cause => { if (!cancelled) setError(cause.message || 'Unable to load projects.') })
    return () => { cancelled = true }
  }, [user.id, user.role, kind, retry])
  const reviewed = kind === 'requirements'
  const generated = kind === 'generated'
  const visible = (items || []).filter(item => reviewed ? reviewEntered(item, user) : generated ? Boolean(item.savedSRS) : !item.savedSRS)
  const title = reviewed ? 'Reviewed Requirement Interviews' : generated ? 'Generated SRS Documents' : 'SRS Generation'
  return <main className="review-page">
    <div className="panel-header"><div><span className="section-label">Saved Work</span><h1>{title}</h1>
      <p>{reviewed ? 'Open the requirements for a specific interview that has entered review.' : generated ? 'View and review the documents generated for your assigned projects.' : 'Select an assigned project to generate its specification after stakeholder finalization.'}</p></div></div>
    {error ? <div className="workspace-error" role="alert">{error}<button className="secondary-button" onClick={() => setRetry(value => value + 1)}>Retry</button></div>
      : !items ? <div className="review-empty-card" role="status">Loading projects…</div>
      : !visible.length ? <div className="review-empty-card">{reviewed ? 'No interviews have entered requirement review yet.' : generated ? 'No generated SRS documents yet.' : 'No projects waiting for SRS generation.'}</div>
      : <div className="features-section">{visible.map(item => <article className="feature-card" key={projectId(item)}>
        <h2>{item.projectName || 'Untitled Project'}</h2><p>Stakeholder: <strong>{item.stakeholderName || 'Not provided'}</strong></p>
        <p>{reviewed ? isFinalized(item, user) ? 'Completed Interview' : 'Requirements in Review' : generated ? `SRS ${item.savedSRS.status || 'GENERATED'}` : 'SRS Not Generated'}</p>
        {generated && <><p>Assigned Business Analyst: {user.name}</p><p>Generated by: {item.savedSRS.generatedByName || item.savedSRS.businessAnalystName || 'Not recorded'}</p><p>Generated at: {item.savedSRS.generatedAt || 'Not provided'}</p></>}
        <Link className="primary-button" to={`${reviewed ? '/requirements' : generated ? '/generated-srs' : '/srs'}/${encodeURIComponent(projectId(item))}`}>
          {reviewed ? 'View Requirements' : generated ? 'View SRS' : 'Open SRS'}</Link>
      </article>)}</div>}
  </main>
}
// Explicit URL selection is checked against the signed-in user's project list
// before any requirements or SRS detail component is mounted.
function ProjectPage(props) {
  const { id } = useParams()
  return <SelectedProject key={`${props.user.id}:${props.user.role}:${props.kind}:${id}`} {...props} />
}
function SelectedProject({ user, kind, onFinalized, onSelectInterview, onRequirementsChanged }) {
  const { id } = useParams()
  const [item, setItem] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => {
    let cancelled = false
    async function load() {
      const items = await ownedProjects(user)
      if (!items.some(value => projectId(value) === id)) throw new Error('This project is not available to your account.')
      const detail = await getInterview(id)
      if (projectId(detail) !== id) throw new Error('The server returned a different project.')
      try {
        const document = await projectSRS(id)
        detail.savedSRS = document || null
      } catch (cause) { if (!missingSRS(cause)) throw cause }
      if (kind === 'interview' && isFinalized(detail, user)) throw new Error('Completed Interview. View its requirements under Requirements.')
      if (kind === 'requirements' && !reviewEntered(detail, user)) throw new Error('Complete discovery and enter Review Requirements from this interview first.')
      if (kind === 'generated' && !detail.savedSRS) throw new Error('No generated SRS is available for this project.')
      if (!cancelled) setItem(detail)
    }
    load().catch(cause => { if (!cancelled) setError(cause.message || 'Unable to load project.') })
    return () => { cancelled = true }
  }, [user.id, user.role, id, kind])
  if (error) return <main className="review-page"><div className="workspace-error" role="alert">{error}</div><Link to="/">Back to Dashboard</Link></main>
  if (!item) return <main className="review-page"><div className="review-empty-card" role="status">Checking selected project…</div></main>
  if (kind === 'interview') return <Interview user={user} activeInterviewId={id} onSelectInterview={onSelectInterview} onRequirementsChanged={onRequirementsChanged} />
  if (kind === 'requirements') return <RequirementReview activeInterviewId={id} finalized={isFinalized(item, user)} onFinalized={onFinalized} />
  return <SRSPage user={user} activeInterviewId={id} viewDocument={kind === 'generated'} />
}
// ======================================================
// Home
// ======================================================
function Home({ user, onSelectInterview }) {
  const analyst = user.role === 'BUSINESS_ANALYST'
  const navigate = useNavigate()
  const location = useLocation()
  const [interviews, setInterviews] = useState([])
  const [loadingInterviews, setLoadingInterviews] = useState(true)
  const [interviewsError, setInterviewsError] = useState('')
  const [reloadInterviews, setReloadInterviews] = useState(0)
  useEffect(() => {
    let cancelled = false
    async function loadInterviews() {
      setLoadingInterviews(true)
      setInterviewsError('')
      try {
        if (typeof user.id !== 'string' || !user.id.trim()) {
          throw new Error('Your account ID is unavailable. Please sign in again before loading your projects.')
        }
        const savedInterviews = await loadProjects(user)
        if (!cancelled) setInterviews(savedInterviews)
      } catch (cause) {
        if (!cancelled) {
          setInterviewsError(cause?.message || 'Unable to load saved interviews.')
        }
      } finally {
        if (!cancelled) setLoadingInterviews(false)
      }
    }
    loadInterviews()
    return () => { cancelled = true }
  }, [user.id, user.role, reloadInterviews])
  function handleStartInterview() {
    onSelectInterview('')
    navigate('/interview')
  }
  function handleContinueInterview(id) {
    if (!id) return
    onSelectInterview(id)
    navigate(analyst ? `/srs/${encodeURIComponent(id)}` : `/interview/${encodeURIComponent(id)}`)
  }
  function interviewDate(savedInterview) {
    const value = savedInterview.updatedAt || savedInterview.createdAt ||
      savedInterview.updated_at || savedInterview.created_at
    if (!value) return null
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return null
    return {
      iso: date.toISOString(),
      label: savedInterview.updatedAt || savedInterview.updated_at ? 'Updated' : 'Created',
      text: date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }),
    }
  }
  return (
    <main className="home-page">
      {location.state?.srsGenerated && <div className="srs-success" role="status">{location.state.srsGenerated}</div>}
      <section className="hero-section">
        <div className="badge">
          AI-Powered Requirements Engineering
        </div>
        <h1>
          Intelligent Requirements
          <span> Engineering Assistant</span>
        </h1>
        <p className="hero-description">
          Transform stakeholder conversations into clear,
          complete, and validated software requirements
          using NLP and Large Language Models.
        </p>
        {!analyst && <div className="hero-actions">
          <button
            type="button"
            className="primary-button"
            onClick={handleStartInterview}
          >
            Start Requirement Interview
          </button>
        </div>}
      </section>
      {analyst && <section className="feature-card" aria-labelledby="ba-assignment-code-title">
        <span className="section-label">Stakeholder Registration</span>
        <h2 id="ba-assignment-code-title">Your Business Analyst Assignment Code</h2>
        {user.assignmentCode ? <>
          <p><strong>{user.assignmentCode}</strong></p>
          <p>Share this code with your assigned stakeholders so they can register with you.</p>
        </> : <p>Your assignment code is unavailable. Please log in again to load it.</p>}
      </section>}
      <section className="features-section">
        <div className="feature-card">
          <div className="feature-icon">01</div>
          <h2>Requirement Extraction</h2>
          <p>
            Analyze stakeholder responses and identify
            functional and non-functional requirements.
          </p>
        </div>
        <div className="feature-card">
          <div className="feature-icon">02</div>
          <h2>Coverage & Ambiguity</h2>
          <p>
            Track requirement coverage, identify unclear
            information, and discover missing details.
          </p>
        </div>
        <div className="feature-card">
          <div className="feature-icon">03</div>
          <h2>Adaptive Questions</h2>
          <p>
            Generate intelligent follow-up questions based
            on the current understanding of the project.
          </p>
        </div>
        <div className="feature-card">
          <div className="feature-icon">04</div>
          <h2>Structured SRS</h2>
          <p>
            Validate requirements and organize them into a
            structured Software Requirements Specification.
          </p>
        </div>
      </section>
      <section aria-labelledby="previous-interviews-title" aria-busy={loadingInterviews}>
        <div className="panel-header">
          <div>
            <span className="section-label">Saved Work</span>
            <h2 id="previous-interviews-title">{analyst ? 'Projects and SRS Review' : 'Previous Interviews'}</h2>
            <p>{analyst ? 'Generate specifications for assigned projects; view saved documents under Generated SRS.' : 'Continue a saved requirement interview.'}</p>
          </div>
        </div>
        {loadingInterviews ? (
          <div className="review-empty-card" role="status">Loading saved interviews…</div>
        ) : interviewsError ? (
          <div className="workspace-error" role="alert">
            <p>{interviewsError}</p>
            <button type="button" className="secondary-button"
              onClick={() => setReloadInterviews((value) => value + 1)}>
              Retry Loading Interviews
            </button>
          </div>
        ) : interviews.length === 0 ? (
          <div className="review-empty-card">
            <p>No saved projects yet.</p>
          </div>
        ) : (
          <div className="features-section">
            {interviews.map((savedInterview, index) => {
              const id = savedInterview.id || savedInterview._id
              const date = interviewDate(savedInterview)
              return (
                <article className="feature-card" key={id || index}>
                  <h2>{savedInterview.projectName || 'Untitled Project'}</h2>
                  <p>Stakeholder: <strong>{savedInterview.stakeholderName || 'Not provided'}</strong></p>
                  {date && <p>{date.label}: <time dateTime={date.iso}>{date.text}</time></p>}
                  <p>{analyst ? savedInterview.savedSRS ? 'SRS Generated' : 'SRS Not Generated' : isFinalized(savedInterview, user) ? 'Completed Interview' : 'Interview in Progress'}</p>
                  <button type="button" className="primary-button"
                    disabled={!id || (analyst && Boolean(savedInterview.savedSRS)) || (!analyst && isFinalized(savedInterview, user))} onClick={() => handleContinueInterview(id)}>
                    {analyst ? savedInterview.savedSRS ? 'SRS Generated' : 'Open SRS' : isFinalized(savedInterview, user) ? 'Completed Interview' : 'Continue Interview'}
                  </button>
                </article>
              )
            })}
          </div>
        )}
      </section>
    </main>
  )
}
// ======================================================
// Interview
// ======================================================
function Interview({ user, activeInterviewId, onSelectInterview, onRequirementsChanged }) {
  const mounted = useRef(false)
  useEffect(() => {
    mounted.current = true
    return () => { mounted.current = false }
  }, [])
  const navigate = useNavigate()
  const [projectName, setProjectName] = useState('')
  const [stakeholderName, setStakeholderName] =
    useState('')
  const [interview, setInterview] = useState(null)
  const [responseText, setResponseText] = useState('')
  const [analysis, setAnalysis] = useState(null)
  const [requirementsExpanded, setRequirementsExpanded] = useState(false)
  const [creating, setCreating] = useState(false)
  const [analyzing, setAnalyzing] = useState(false)
  const [restoring, setRestoring] = useState(true)
  const [error, setError] = useState('')
  // ====================================================
  // Restore interview
  // ====================================================
  useEffect(() => {
    let cancelled = false
    async function restoreInterview() {
      const storedInterviewId = activeInterviewId
      if (!storedInterviewId) {
        setRestoring(false)
        return
      }
      try {
        setRestoring(true)
        const owned = await ownedProjects(user)
        if (!owned.some(item => projectId(item) === storedInterviewId)) throw new Error('This interview is not available to your account.')
        const restoredInterview =
          await getInterview(storedInterviewId)
        if (cancelled) {
          return
        }
        if (projectId(restoredInterview) !== storedInterviewId) throw new Error('The server returned a different interview.')
        try { restoredInterview.savedSRS = await projectSRS(storedInterviewId) }
        catch (cause) { if (!missingSRS(cause)) throw cause }
        if (cancelled) return
        if (isFinalized(restoredInterview, user)) throw new Error('Completed Interview. Open its requirements from the Requirements tab.')
        setInterview(restoredInterview)
        setAnalysis(null)
        setError('')
      } catch (cause) {
        if (cancelled) {
          return
        }
        setError(cause?.message || 'Unable to restore the selected interview. Return to Dashboard and retry.')
        setInterview(null)
      } finally {
        if (!cancelled) {
          setRestoring(false)
        }
      }
    }
    restoreInterview()
    return () => {
      cancelled = true
    }
  }, [])
  // ====================================================
  // Create interview
  // ====================================================
  async function handleCreateInterview(event) {
    event.preventDefault()
    if (
      !projectName.trim() ||
      !stakeholderName.trim()
    ) {
      setError(
        'Please enter both the project name and stakeholder name.',
      )
      return
    }
    if (creating) return
    if (!user?.id) {
      setError('Your account ID is unavailable. Please sign in again before creating an interview.')
      return
    }
    if (!user.assignedBusinessAnalystId || !String(user.assignedBusinessAnalystId).trim()) {
      setError('Your account has no assigned Business Analyst. Please contact your administrator to assign a Business Analyst before creating an interview.')
      return
    }
    try {
      setCreating(true)
      setError('')
      const createdInterview =
        await createInterview(
          projectName.trim(),
          stakeholderName.trim(),
          user.id,
          user.assignedBusinessAnalystId,
        )
      if (!mounted.current) return
      if (!createdInterview?.id && !createdInterview?._id) {
        throw new Error('The server did not return an interview ID.')
      }
      setInterview(createdInterview)
      setAnalysis(null)
      setResponseText('')
      onSelectInterview(createdInterview.id || createdInterview._id)
    } catch (err) {
      setError(
        err.message ||
          'Unable to create the interview.',
      )
    } finally {
      setCreating(false)
    }
  }
  // ====================================================
  // Send stakeholder response
  // ====================================================
  async function handleSubmitResponse(event) {
    event.preventDefault()
    if (analyzing || !responseText.trim() || !interview) {
      return
    }
    const stakeholderResponse =
      responseText.trim()
    try {
      setAnalyzing(true)
      setError('')
      const result =
        await sendStakeholderResponse(
          interview.id,
          stakeholderResponse,
        )
      if (!mounted.current) return
      onRequirementsChanged(activeInterviewId)
      setInterview(result.interview)
      setAnalysis(result.analysis)
      setResponseText('')

    } catch (err) {
      setError(
        err.message ||
          'Unable to analyze the stakeholder response.',
      )
    } finally {
      setAnalyzing(false)
    }
  }
  // ====================================================
  // New interview
  // ====================================================
  function handleStartOver() {
    navigate('/interview')
    onSelectInterview('')
    setInterview(null)
    setAnalysis(null)
    setProjectName('')
    setStakeholderName('')
    setResponseText('')
    setError('')
  }
  // ====================================================
  // Restoring screen
  // ====================================================
  if (restoring) {
    return (
      <main className="interview-page">
        <section className="interview-start-card">
          <div className="start-card-header">
            <div className="start-card-icon">
              AI
            </div>
            <div>
              <h2>Restoring Interview</h2>
              <p>
                Loading your active requirement
                interview...
              </p>
            </div>
          </div>
        </section>
      </main>
    )
  }
  // ====================================================
  // New interview screen
  // ====================================================
  if (!interview && activeInterviewId) {
    return (
      <main className="interview-page">
        <section className="interview-start-card">
          <h2>Unable to load the selected interview</h2>
          <div className="workspace-error" role="alert">{error}</div>
          <Link to="/" className="secondary-button">← Back to Dashboard</Link>
        </section>
      </main>
    )
  }
  if (!interview) {
    return (
      <main className="interview-page">
        <div className="interview-page-header">
          <Link
            to="/"
            className="back-link"
          >
            ← Back to Home
          </Link>
          <div className="interview-heading">
            <span className="section-label">
              Requirement Interview
            </span>
            <h1>
              Start a Stakeholder Interview
            </h1>
            <p>
              Create a project interview and let the
              AI assistant guide the requirements
              discovery process.
            </p>
          </div>
        </div>
        <section className="interview-start-card">
          <div className="start-card-header">
            <div className="start-card-icon">
              AI
            </div>
            <div>
              <h2>Project Information</h2>
              <p>
                Enter the basic details before
                beginning the requirement interview.
              </p>
            </div>
          </div>
          <form
            className="interview-start-form"
            onSubmit={handleCreateInterview}
          >
            <label>
              <span>Project Name</span>
              <input
                type="text"
                value={projectName}
                onChange={(event) =>
                  setProjectName(
                    event.target.value,
                  )
                }
                placeholder="e.g. Library Management System"
                disabled={creating}
              />
            </label>
            <label>
              <span>Stakeholder Name</span>
              <input
                type="text"
                value={stakeholderName}
                onChange={(event) =>
                  setStakeholderName(
                    event.target.value,
                  )
                }
                placeholder="e.g. Library Administrator"
                disabled={creating}
              />
            </label>
            {error && (
              <div className="form-error">
                {error}
              </div>
            )}
            <button
              type="submit"
              className="primary-button start-interview-button"
              disabled={creating}
            >
              {creating
                ? 'Creating Interview...'
                : 'Begin AI Interview'}
            </button>
          </form>
        </section>
      </main>
    )
  }
  // ====================================================
  // Understanding state
  // ====================================================
  const state =
    interview.understandingState
  const nextQuestion =
    analysis?.next_question ||
    state?.nextQuestion ||
    INITIAL_QUESTION
  const coverage =
    analysis?.coverage ||
    state?.coverage ||
    []
  const requirements =
    analysis?.requirements ||
    state?.requirements ||
    []
  const ambiguities =
    analysis?.ambiguities ||
    state?.ambiguities ||
    []
  const missingInformation =
    analysis?.missing_information ||
    state?.missingInformation ||
    []
  const responses =
    interview.responses || []
  const allCoverageComplete =
    COVERAGE_AREAS.every((area) =>
      coverage.some(
        (item) =>
          item.area === area &&
          item.status === 'COVERED',
      ),
    )
  function handleReviewRequirements() {
    if (!allCoverageComplete || analyzing) return
    try {
      saveWorkflow(user, projectId(interview), { reviewEntered: true })
      navigate(`/requirements/${encodeURIComponent(projectId(interview))}`)
    } catch { setError('Unable to save review entry. Allow browser storage and try again.') }
  }
  // ====================================================
  // Active interview
  // ====================================================
  return (
    <main className="workspace-page">
      <section className="workspace-topbar">
        <div>
          <span className="section-label">
            Active Requirement Interview
          </span>
          <h1>
            {interview.projectName}
          </h1>
          <p>
            Stakeholder:{' '}
            <strong>
              {interview.stakeholderName}
            </strong>
          </p>
        </div>
        <button
          type="button"
          className="secondary-button"
          onClick={handleStartOver}
        >
          New Interview
        </button>
      </section>
      {error && (
        <div className="workspace-error">
          {error}
        </div>
      )}
      {allCoverageComplete && (
        <section className="interview-complete-banner">
          <div className="completion-icon">
            ✓
          </div>
          <div className="completion-copy">
            <span>
              Requirement Discovery Complete
            </span>
            <h2>
              All requirement areas are covered.
            </h2>
            <p>
              Review the extracted requirements,
              correct anything that needs adjustment,
              and validate them before finalizing
              the requirement set.
            </p>
          </div>
          <button
            type="button"
            className="primary-button"
            onClick={handleReviewRequirements}
            disabled={analyzing}
          >
            Review Requirements
          </button>
        </section>
      )}
      <section className="workspace-grid">
        <div className="interview-panel">
          <div className="panel-header">
            <div>
              <span className="panel-eyebrow">
                AI Interview Assistant
              </span>
              <h2>
                Stakeholder Conversation
              </h2>
            </div>
            <div className="live-status">
              <span className="live-dot"></span>
              {allCoverageComplete
                ? 'Complete'
                : 'Active'}
            </div>
          </div>
          <div className="conversation-area">
            {responses.length === 0 && (
              <div className="message ai-message">
                <div className="message-avatar">
                  AI
                </div>
                <div className="message-content">
                  <span>
                    AI Assistant
                  </span>
                  <p>
                    {INITIAL_QUESTION}
                  </p>
                </div>
              </div>
            )}
            {responses.map(
              (item, index) => {
                const question =
                  item.question ||
                  (
                    index === 0
                      ? INITIAL_QUESTION
                      : null
                  )
                return (
                  <div
                    className="conversation-turn"
                    key={
                      `${item.timestamp || 'turn'}-${index}`
                    }
                  >
                    {question && (
                      <div className="message ai-message">
                        <div className="message-avatar">
                          AI
                        </div>
                        <div className="message-content">
                          <span>
                            AI Assistant
                          </span>
                          <p>
                            {question}
                          </p>
                        </div>
                      </div>
                    )}
                    <div className="message stakeholder-message">
                      <div className="message-content">
                        <span>
                          {interview.stakeholderName}
                        </span>
                        <p>
                          {item.response}
                        </p>
                      </div>
                      <div className="message-avatar stakeholder-avatar">
                        {
                          interview.stakeholderName
                            ?.charAt(0)
                            ?.toUpperCase() ||
                          'S'
                        }
                      </div>
                    </div>
                  </div>
                )
              },
            )}
            {responses.length > 0 &&
              !analyzing && (
                <div className="message ai-message">
                  <div className="message-avatar">
                    AI
                  </div>
                  <div className="message-content">
                    <span>
                      AI Assistant
                    </span>
                    <p>
                      {nextQuestion}
                    </p>
                  </div>
                </div>
              )}
            {analyzing && (
              <div className="message ai-message">
                <div className="message-avatar">
                  AI
                </div>
                <div className="message-content">
                  <span>
                    AI Assistant
                  </span>
                  <p className="analyzing-text">
                    Analyzing requirements and
                    preparing the next question...
                  </p>
                </div>
              </div>
            )}
          </div>
          <form
            className="response-composer"
            onSubmit={handleSubmitResponse}
          >
            <textarea
              value={responseText}
              onChange={(event) =>
                setResponseText(
                  event.target.value,
                )
              }
              placeholder={
                allCoverageComplete
                  ? 'You may add an additional requirement, or continue to Requirement Review.'
                  : 'Describe your requirement or answer the AI question...'
              }
              rows="4"
              disabled={analyzing}
            />
            <div className="composer-footer">
              <span>
                {allCoverageComplete
                  ? 'All seven requirement areas are covered. You can add more information or continue to validation.'
                  : 'Your response will be analyzed for requirements, ambiguity, and coverage.'}
              </span>
              <button
                type="submit"
                className="primary-button"
                disabled={
                  analyzing ||
                  !responseText.trim()
                }
              >
                {analyzing
                  ? 'Analyzing...'
                  : 'Send Response'}
              </button>
            </div>
          </form>
        </div>
        <aside className="analysis-sidebar">
          <div className="analysis-card">
            <div className="analysis-card-heading">
              <span>
                Coverage
              </span>
              <strong>
                {coverage.length} Areas
              </strong>
            </div>
            {coverage.length === 0 ? (
              <p className="empty-analysis">
                Coverage analysis will appear
                after the first response.
              </p>
            ) : (
              <div className="coverage-list">
                {coverage.map(
                  (item) => (
                    <div
                      className="coverage-item"
                      key={item.area}
                    >
                      <div>
                        <span>
                          {item.area}
                        </span>
                        <small>
                          {item.status}
                        </small>
                      </div>
                      <div
                        className={
                          `coverage-status ${String(
                            item.status,
                          ).toLowerCase()}`
                        }
                      ></div>
                    </div>
                  ),
                )}
              </div>
            )}
          </div>
          <div className="analysis-card">
            <div className="analysis-card-heading">
              <span>
                Requirements
              </span>
              <strong>
                {requirements.length}
              </strong>
            </div>
            {requirements.length === 0 ? (
              <p className="empty-analysis">
                Extracted requirements will
                appear here.
              </p>
            ) : (
              <div className="requirement-preview-list">
                {requirements
                  .slice(0, requirementsExpanded ? requirements.length : 5)
                  .map(
                    (
                      requirement,
                      index,
                    ) => (
                      <div
                        className="requirement-preview"
                        key={index}
                      >
                        <span
                          className={
                            `requirement-type ${String(
                              requirement.type,
                            ).toLowerCase()}`
                          }
                        >
                          {
                            requirement.type ===
                            'NON_FUNCTIONAL'
                              ? 'NFR'
                              : 'FR'
                          }
                        </span>
                        <p>
                          {requirement.text}
                        </p>
                      </div>
                    ),
                  )}
                {requirements.length > 5 && (
                  <button
                    type="button"
                    className="more-requirements"
                    aria-expanded={requirementsExpanded}
                    onClick={() => setRequirementsExpanded((expanded) => !expanded)}
                  >
                    {requirementsExpanded
                      ? 'Show less'
                      : `+ ${requirements.length - 5} more requirement${requirements.length - 5 === 1 ? '' : 's'}`}
                  </button>
                )}
              </div>
            )}
          </div>
          <div className="analysis-card">
            <div className="analysis-card-heading">
              <span>
                Analysis
              </span>
              <strong>
                Live
              </strong>
            </div>
            <div className="analysis-stat">
              <span>
                Ambiguities
              </span>
              <strong>
                {ambiguities.length}
              </strong>
            </div>
            <div className="analysis-stat">
              <span>
                Missing Information
              </span>
              <strong>
                {missingInformation.length}
              </strong>
            </div>
          </div>
        </aside>
      </section>
    </main>
  )
}
// ======================================================
// Requirement Review
// ======================================================
function RequirementReview({ activeInterviewId, finalized, onFinalized }) {
  const navigate = useNavigate()
  const [interview, setInterview] = useState(null)
  const [requirements, setRequirements] =
    useState([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)
  const [editingId, setEditingId] =
    useState(null)
  const [editText, setEditText] =
    useState('')
  const [editType, setEditType] =
    useState('FUNCTIONAL')
  const [finalizing, setFinalizing] =
    useState(false)
  const [error, setError] =
    useState('')
  const [newRequirementText, setNewRequirementText] = useState('')
  const [newRequirementType, setNewRequirementType] = useState('FUNCTIONAL')
  const [addingRequirement, setAddingRequirement] = useState(false)
  // ====================================================
  // Load persisted requirements
  // ====================================================
  useEffect(() => {
    let cancelled = false
    async function loadReview() {
      const interviewId = activeInterviewId
      if (!interviewId) {
        if (!cancelled) {
          setError(
            'No active interview was found.',
          )
          setLoading(false)
        }
        return
      }
      try {
        const [
          interviewResult,
          requirementResult,
        ] = await Promise.all([
          getInterview(interviewId),
          getRequirements(interviewId),
        ])
        if (cancelled) {
          return
        }
        if (projectId(interviewResult) !== interviewId) throw new Error('The server returned a different interview.')
        setInterview(interviewResult)
        setRequirements(
          Array.isArray(requirementResult)
            ? requirementResult
            : [],
        )
      } catch (err) {
        if (cancelled) {
          return
        }
        setError(
          err.message ||
            'Unable to load requirements.',
        )
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    loadReview()
    return () => {
      cancelled = true
    }
  }, [])
  // ====================================================
  // Requirement helpers
  // ====================================================
  const validatedCount =
    requirements.filter(
      (requirement) =>
        requirement.status ===
        'VALIDATED',
    ).length
  const allValidated =
    requirements.length > 0 &&
    validatedCount === requirements.length
  function beginEditing(requirement) {
    if (finalized || finalizing || busyId !== null || addingRequirement) return
    setEditingId(requirement.id)
    setEditText(requirement.text || '')
    setEditType(
      requirement.type || 'FUNCTIONAL',
    )
    setError('')
  }
  function cancelEditing() {
    setEditingId(null)
    setEditText('')
    setEditType('FUNCTIONAL')
  }
  // ====================================================
  // Add a stakeholder requirement
  // ====================================================
  async function handleAddRequirement(event) {
    event.preventDefault()
    if (addingRequirement || finalizing || finalized || busyId !== null) {
      return
    }
    const trimmedText = newRequirementText.trim()
    if (!trimmedText) {
      setError('Requirement text cannot be empty.')
      return
    }
    if (!interview?.id) {
      setError('No active interview was found.')
      return
    }
    try {
      setAddingRequirement(true)
      setError('')
      const requirement = await addRequirement(
        interview.id,
        trimmedText,
        newRequirementType,
      )
      setRequirements((current) => [...current, requirement])
      setNewRequirementText('')
      setNewRequirementType('FUNCTIONAL')
    } catch (err) {
      setError(err.message || 'Unable to add requirement.')
    } finally {
      setAddingRequirement(false)
    }
  }
  // ====================================================
  // Validate requirement
  // ====================================================
  async function handleDelete(requirementId) {
    if (finalized || finalizing || busyId !== null || addingRequirement) return
    if (!window.confirm('Delete this requirement permanently? This action cannot be undone.')) return
    setBusyId(requirementId)
    setError('')
    try {
      await deleteRequirement(requirementId)
      setRequirements((current) =>
        current.filter((requirement) => requirement.id !== requirementId),
      )
      if (editingId === requirementId) cancelEditing()
    } catch (err) {
      setError(err?.message || 'Unable to delete requirement.')
    } finally {
      setBusyId(null)
    }
  }
  async function handleValidate(requirementId) {
    if (finalized || finalizing || busyId !== null || addingRequirement) return
    try {
      setBusyId(requirementId)
      setError('')
      const updated =
        await validateRequirement(
          requirementId,
        )
      setRequirements((current) =>
        current.map((requirement) =>
          requirement.id ===
          updated.id
            ? updated
            : requirement,
        ),
      )
    } catch (err) {
      setError(
        err.message ||
          'Unable to validate requirement.',
      )
    } finally {
      setBusyId(null)
    }
  }
  // ====================================================
  // Save correction
  // ====================================================
  async function handleSaveCorrection(
    requirementId,
  ) {
    if (finalized || finalizing || busyId !== null || addingRequirement) return
    if (!editText.trim()) {
      setError(
        'Requirement text cannot be empty.',
      )
      return
    }
    try {
      setBusyId(requirementId)
      setError('')
      const updated =
        await correctRequirement(
          requirementId,
          editText.trim(),
          editType,
        )
      setRequirements((current) =>
        current.map((requirement) =>
          requirement.id ===
          updated.id
            ? updated
            : requirement,
        ),
      )
      cancelEditing()
    } catch (err) {
      setError(
        err.message ||
          'Unable to correct requirement.',
      )
    } finally {
      setBusyId(null)
    }
  }
  // ====================================================
  // Finalize requirements
  // ====================================================
  async function handleFinalize() {
    if (!interview?.id || !allValidated || finalized || finalizing || busyId !== null || addingRequirement) {
      return
    }
    try {
      setFinalizing(true)
      setError('')
      const result =
        await finalizeRequirements(
          interview.id,
        )
      if (Array.isArray(result)) {
        setRequirements(result)
      }
      cancelEditing()
      onFinalized(activeInterviewId)
    } catch (err) {
      setError(
        err.message ||
          'Unable to finalize requirements.',
      )
    } finally {
      setFinalizing(false)
    }
  }
  // ====================================================
  // Loading
  // ====================================================
  if (loading) {
    return (
      <main className="review-page">
        <section className="review-loading-card">
          <div className="start-card-icon">
            RE
          </div>
          <div>
            <h2>
              Loading Requirements
            </h2>
            <p>
              Preparing the stakeholder
              validation workspace...
            </p>
          </div>
        </section>
      </main>
    )
  }
  // ====================================================
  // Review UI
  // ====================================================
  return (
    <main className="review-page">
      <section className="review-header">
        <div>
          <button
            type="button"
            className="back-button"
            onClick={() =>
              navigate(finalized ? '/requirements' : `/interview/${encodeURIComponent(activeInterviewId)}`)
            }
          >
            ← Back to Interview
          </button>
          <span className="section-label">
            Stakeholder Validation
          </span>
          <h1>
            Review Requirements
          </h1>
          <p>
            Review the extracted requirements,
            correct any inaccurate information,
            and validate each requirement before
            finalizing it for Business Analyst
            and SRS processing.
          </p>
        </div>
        {interview && (
          <div className="review-project-card">
            <span>
              Current Project
            </span>
            <strong>
              {interview.projectName}
            </strong>
            <small>
              Stakeholder:{' '}
              {interview.stakeholderName}
            </small>
          </div>
        )}
      </section>
      {error && (
        <div className="workspace-error">
          {error}
        </div>
      )}
      <section className="review-summary-grid">
        <div className="review-summary-card">
          <span>
            Total Requirements
          </span>
          <strong>
            {requirements.length}
          </strong>
          <small>
            Extracted from the interview
          </small>
        </div>
        <div className="review-summary-card">
          <span>
            Functional
          </span>
          <strong>
            {
              requirements.filter(
                (requirement) =>
                  requirement.type ===
                  'FUNCTIONAL',
              ).length
            }
          </strong>
          <small>
            System behavior and features
          </small>
        </div>
        <div className="review-summary-card">
          <span>
            Non-Functional
          </span>
          <strong>
            {
              requirements.filter(
                (requirement) =>
                  requirement.type ===
                  'NON_FUNCTIONAL',
              ).length
            }
          </strong>
          <small>
            Quality and constraints
          </small>
        </div>
        <div className="review-summary-card validated-summary">
          <span>
            Validated
          </span>
          <strong>
            {validatedCount}
            <small>
              /{requirements.length}
            </small>
          </strong>
          <small>
            Confirmed by stakeholder
          </small>
        </div>
      </section>
      <section className="validation-progress-card">
        <div>
          <span>
            Validation Progress
          </span>
          <strong>
            {requirements.length === 0
              ? 0
              : Math.round(
                  (
                    validatedCount /
                    requirements.length
                  ) * 100,
                )}
            %
          </strong>
        </div>
        <div className="validation-progress-track">
          <div
            className="validation-progress-fill"
            style={{
              width:
                requirements.length === 0
                  ? '0%'
                  : `${
                      (
                        validatedCount /
                        requirements.length
                      ) * 100
                    }%`,
            }}
          ></div>
        </div>
      </section>
      <section
        className="review-finalization-card"
        aria-labelledby="add-requirement-heading"
        style={{ display: 'block', marginBottom: '24px' }}
      >
        <div style={{ marginBottom: '20px' }}>
          <span className="panel-eyebrow">Stakeholder Input</span>
          <h2 id="add-requirement-heading">Add Requirement</h2>
          <p id="add-requirement-description">
            Capture anything the interview missed. Add a system feature or
            quality constraint, then review and validate it with the requirements below.
          </p>
        </div>
        <form
          className="requirement-editor"
          onSubmit={handleAddRequirement}
          aria-describedby="add-requirement-description"
          aria-busy={addingRequirement}
        >
          <label htmlFor="new-requirement-text">
            <span>Requirement</span>
            <textarea
              id="new-requirement-text"
              value={newRequirementText}
              onChange={(event) => setNewRequirementText(event.target.value)}
              rows="4"
              placeholder="The system shall…"
              disabled={!interview?.id || addingRequirement || finalizing || finalized || busyId !== null}
            />
          </label>
          <label htmlFor="new-requirement-type">
            <span>Requirement Type</span>
            <select
              id="new-requirement-type"
              value={newRequirementType}
              onChange={(event) => setNewRequirementType(event.target.value)}
              disabled={!interview?.id || addingRequirement || finalizing || finalized || busyId !== null}
            >
              <option value="FUNCTIONAL">Functional Requirement</option>
              <option value="NON_FUNCTIONAL">Non-Functional Requirement</option>
            </select>
          </label>
          <div className="editor-actions">
            <button
              type="submit"
              className="primary-button"
              disabled={!interview?.id || addingRequirement || finalizing || finalized || busyId !== null}
            >
              {addingRequirement ? 'Saving…' : '+ Save Requirement'}
            </button>
          </div>
        </form>
      </section>
      {requirements.length === 0 ? (
        <section className="review-empty-card">
          <h2>
            No requirements found
          </h2>
          <p>
            Complete the requirement interview
            before starting stakeholder
            validation.
          </p>
          <button
            type="button"
            className="primary-button"
            onClick={() =>
              navigate(finalized ? '/requirements' : `/interview/${encodeURIComponent(activeInterviewId)}`)
            }
          >
            Return to Interview
          </button>
        </section>
      ) : (
        <section className="requirements-review-list">
          {requirements.map(
            (requirement, index) => {
              const isEditing =
                editingId === requirement.id
              const isBusy =
                busyId === requirement.id
              const isValidated =
                requirement.status ===
                'VALIDATED'
              return (
                <article
                  className={
                    `review-requirement-card ${
                      isValidated
                        ? 'validated'
                        : ''
                    }`
                  }
                  key={requirement.id}
                >
                  <div className="requirement-number">
                    {String(
                      index + 1,
                    ).padStart(2, '0')}
                  </div>
                  <div className="review-requirement-content">
                    <div className="review-requirement-topline">
                      <div className="review-tags">
                        <span
                          className={
                            `requirement-type ${String(
                              requirement.type,
                            ).toLowerCase()}`
                          }
                        >
                          {
                            requirement.type ===
                            'NON_FUNCTIONAL'
                              ? 'NFR'
                              : 'FR'
                          }
                        </span>
                        <span
                          className={
                            `requirement-status ${
                              isValidated
                                ? 'validated'
                                : 'pending'
                            }`
                          }
                        >
                          {isValidated
                            ? '✓ Validated'
                            : 'Pending Validation'}
                        </span>
                        {requirement.stakeholderModified && (
                          <span className="modified-tag">
                            Stakeholder Corrected
                          </span>
                        )}
                      </div>
                    </div>
                    {isEditing ? (
                      <div className="requirement-editor">
                        <label>
                          <span>
                            Requirement
                          </span>
                          <textarea
                            value={editText}
                            onChange={(event) =>
                              setEditText(
                                event.target.value,
                              )
                            }
                            rows="4"
                            disabled={busyId !== null || addingRequirement || finalized || finalizing}
                          />
                        </label>
                        <label>
                          <span>
                            Requirement Type
                          </span>
                          <select
                            value={editType}
                            onChange={(event) =>
                              setEditType(
                                event.target.value,
                              )
                            }
                            disabled={busyId !== null || addingRequirement || finalized || finalizing}
                          >
                            <option value="FUNCTIONAL">
                              Functional Requirement
                            </option>
                            <option value="NON_FUNCTIONAL">
                              Non-Functional Requirement
                            </option>
                          </select>
                        </label>
                        <div className="editor-actions">
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => handleDelete(requirement.id)}
                            disabled={busyId !== null || addingRequirement || finalized || finalizing}
                          >
                            Delete Requirement
                          </button>
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={cancelEditing}
                            disabled={busyId !== null || addingRequirement || finalized || finalizing}
                          >
                            Cancel
                          </button>
                          <button
                            type="button"
                            className="primary-button"
                            onClick={() =>
                              handleSaveCorrection(
                                requirement.id,
                              )
                            }
                            disabled={busyId !== null || addingRequirement || finalized || finalizing}
                          >
                            {isBusy
                              ? 'Saving...'
                              : 'Save Correction'}
                          </button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <p className="review-requirement-text">
                          {requirement.text}
                        </p>
                        {requirement.stakeholderModified &&
                          requirement.originalText &&
                          requirement.originalText !==
                            requirement.text && (
                            <div className="original-requirement">
                              <span>
                                Original AI Extraction
                              </span>
                              <p>
                                {
                                  requirement.originalText
                                }
                              </p>
                            </div>
                          )}
                        <div className="review-requirement-actions">
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => handleDelete(requirement.id)}
                            disabled={busyId !== null || addingRequirement || finalized || finalizing}
                          >
                            Delete Requirement
                          </button>
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() =>
                              beginEditing(
                                requirement,
                              )
                            }
                            disabled={busyId !== null || addingRequirement || finalized || finalizing}
                          >
                            Edit / Correct
                          </button>
                          {!isValidated && (
                            <button
                              type="button"
                              className="validate-button"
                              onClick={() =>
                                handleValidate(
                                  requirement.id,
                                )
                              }
                              disabled={busyId !== null || addingRequirement || finalized || finalizing}
                            >
                              {isBusy
                                ? 'Validating...'
                                : '✓ Validate Requirement'}
                            </button>
                          )}
                        </div>
                      </>
                    )}
                  </div>
                </article>
              )
            },
          )}
        </section>
      )}
      {requirements.length > 0 && (
        <section className="review-finalization-card">
          <div>
            <span className="panel-eyebrow">
              Requirement Validation
            </span>
            <h2>
              {allValidated
                ? 'All requirements are validated.'
                : `${validatedCount} of ${requirements.length} requirements validated`}
            </h2>
            <p>
              {finalized
                ? 'The requirements are finalized and ready for Business Analyst and SRS processing.'
                : allValidated
                ? 'The validated requirement set is ready to be finalized and used for SRS generation.'
                : 'Review and validate every requirement before finalizing the requirement set.'}
            </p>
          </div>
          {finalized ? (
            <div className="srs-success" role="status">
              ✓ Requirements finalized successfully. Ready for Business Analyst
              and SRS processing.
            </div>
          ) : (
            <button
              type="button"
              className="primary-button"
              onClick={handleFinalize}
              disabled={
                !allValidated ||
                finalizing || busyId !== null || addingRequirement
              }
            >
              {finalizing
                ? 'Finalizing...'
                : 'Finalize Requirements'}
            </button>
          )}
        </section>
      )}
    </main>
  )
}
// ======================================================
// SRS generation and Business Analyst review
// ======================================================
function SRSPage({ user, activeInterviewId, viewDocument = false }) {
  const navigate = useNavigate()
  const analyst = user.role === 'BUSINESS_ANALYST'
  const [interviewId, setInterviewId] = useState('')
  const [interview, setInterview] = useState(null)
  const [srs, setSrs] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [loadNotice, setLoadNotice] = useState('')
  const [success, setSuccess] = useState('')
  // api.js exposes error messages rather than HTTP status codes.
  const isMissingSRS = missingSRS
  const mounted = useRef(false)
  useEffect(() => {
    mounted.current = true
    return () => { mounted.current = false }
  }, [])
  useEffect(() => {
    let cancelled = false
    async function loadWorkspace() {
      try {
        const activeId = activeInterviewId
        if (!activeId) {
          if (!cancelled) setError('No active project was found. Return to Dashboard and select a project to open its SRS.')
          return
        }
        if (!cancelled) setInterviewId(activeId)
        const interviewResult = await getInterview(activeId)
        if (cancelled) return
        if (projectId(interviewResult) !== activeId) throw new Error('The server returned a different interview.')
        setInterview(interviewResult)
        try {
          const result = await projectSRS(activeId)
          if (!cancelled) setSrs(result || null)
        } catch (cause) {
          if (!cancelled && !isMissingSRS(cause)) {
            setLoadNotice(`The saved SRS could not be loaded: ${cause.message || 'Please try again.'}`)
          }
          // An interview without an SRS is a normal first-visit state.
        }
      } catch (cause) {
        if (!cancelled) setError(cause.message || 'Unable to load the active interview.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadWorkspace()
    return () => { cancelled = true }
  }, [])
  const status = String(srs?.status || (srs ? 'GENERATED' : 'NOT GENERATED')).toUpperCase()
  const reviewed = status === 'REVIEWED'
  function requirementItems(value) {
    if (Array.isArray(value)) return value
    if (typeof value === 'string') return value.split(/\r?\n/).filter((line) => line.trim())
    return []
  }
  function requirementText(item) {
    if (typeof item === 'string') return item
    return item?.text || item?.description || item?.requirement || item?.title || ''
  }
  const functional = requirementItems(srs?.functionalRequirements)
  const nonFunctional = requirementItems(srs?.nonFunctionalRequirements)
  const unspecified = 'Not specified by stakeholder'
  function displayValue(value) {
    if (typeof value === 'string') return value.trim() ? value : unspecified
    if (typeof value === 'number') return value
    return unspecified
  }
  function renderParagraphs(value) {
    const paragraphs = typeof value === 'string'
      ? value.split(/\n\s*\n/).filter((paragraph) => paragraph.trim()) : []
    return paragraphs.length
      ? paragraphs.map((paragraph, index) => <p className="srs-paragraph" style={{ whiteSpace: 'pre-wrap' }} key={index}>{paragraph}</p>)
      : <p className="srs-section-empty">{unspecified}</p>
  }
  function renderRequirements(value, prefix) {
    const items = requirementItems(value)
    if (!items.length) return <p className="srs-section-empty">{unspecified}</p>
    return <ol className="srs-requirements-list" style={prefix ? { listStyle: 'none', paddingLeft: 0 } : undefined}>
      {items.map((item, index) => <li className="srs-requirement" key={index}>
        {prefix && <strong>{prefix}-{String(index + 1).padStart(2, '0')} — </strong>}
        {displayValue(requirementText(item))}
      </li>)}
    </ol>
  }
  const coverage = Array.isArray(srs?.coverage) ? srs.coverage : []
  const ambiguityCount = Array.isArray(srs?.ambiguities) ? srs.ambiguities.length : null
  const missingInformationCount = Array.isArray(srs?.missingInformation) ? srs.missingInformation.length : null
  async function handleReload() {
    if (!interviewId || busy) return
    setBusy('reload')
    setError('')
    setLoadNotice('')
    setSuccess('')
    try {
      const document = await projectSRS(interviewId)
      if (!mounted.current) return
      setSrs(document || null)
    } catch (cause) {
      if (isMissingSRS(cause)) setSrs(null)
      else setLoadNotice(cause.message || 'Unable to load the saved SRS. Try again.')
    } finally {
      setBusy('')
    }
  }
  async function handleGenerate() {
    if (!analyst || !interviewId || !interview || busy || reviewed || srs || loadNotice) return
    setBusy('generate')
    setError('')
    setSuccess('')
    try {
      const result = await generateSRS(interviewId)
      const document = result || await projectSRS(interviewId)
      if (!mounted.current) return
      if (document?.interviewId && String(document.interviewId) !== interviewId) throw new Error('The server returned an SRS for a different project.')
      if (!document) throw new Error('The generated SRS was not returned. Reload the saved SRS to check its status.')
      navigate('/', { replace: true, state: { srsGenerated: 'SRS generated successfully. View the document under Generated SRS.' } })
    } catch (cause) {
      setError(cause.message || 'Unable to generate the SRS. Please try again.')
    } finally {
      setBusy('')
    }
  }
  async function handleReview() {
    if (!analyst || !interviewId || !srs || busy || reviewed) return
    setBusy('review')
    setError('')
    setSuccess('')
    try {
      const result = await reviewSRS(interviewId)
      const document = result || await projectSRS(interviewId)
      if (!mounted.current) return
      if (document?.interviewId && String(document.interviewId) !== interviewId) throw new Error('The server returned an SRS for a different project.')
      if (!document) throw new Error('Unable to confirm the review status. Reload the saved SRS to check it.')
      setSrs(document)
      setLoadNotice('')
      setSuccess(String(document.status).toUpperCase() === 'REVIEWED'
        ? 'Business Analyst review completed successfully.'
        : 'Review request completed. The current SRS status is shown below.')
    } catch (cause) {
      setError(cause.message || 'Unable to mark the SRS as reviewed. Please try again.')
    } finally {
      setBusy('')
    }
  }
  return (
    <main className="srs-page review-page" aria-busy={loading || Boolean(busy)}>
      <header className="srs-header">
        <div className="srs-heading">
          <div className="srs-badge badge">Software Requirements Specification</div>
          <h1 className="srs-title">SRS Review</h1>
          <p className="srs-description">Generate or review the specification for the selected project after stakeholder finalization.</p>
        </div>
        <Link to="/" className="srs-back-link secondary-button">← Back to Projects</Link>
      </header>
      {loading ? (
        <section className="srs-loading review-empty-card" role="status">Loading the active interview and SRS…</section>
      ) : (
        <>
          {error && <div className="srs-error" role="alert">{error}</div>}
          {loadNotice && <div className="srs-notice" role="status">{loadNotice} You can retry loading the saved document.</div>}
          {success && <div className="srs-success" role="status">{success}</div>}
          {interview && (
            <>
              <section className="srs-summary">
                <h2 className="srs-project-name">{displayValue(srs ? srs.projectName : interview.projectName)}</h2>
                <dl className="srs-metadata">
                  <div className="srs-metadata-item"><dt>Stakeholder</dt><dd>{displayValue(srs ? srs.stakeholderName : interview.stakeholderName)}</dd></div>
                  <div className="srs-metadata-item"><dt>Interview ID</dt><dd>{displayValue(srs ? srs.interviewId : interviewId)}</dd></div>
                  {srs && <>
                    <div className="srs-metadata-item"><dt>SRS ID</dt><dd>{displayValue(srs.id)}</dd></div>
                    <div className="srs-metadata-item"><dt>Generated at</dt><dd>{displayValue(srs.generatedAt)}</dd></div>
                    <div className="srs-metadata-item"><dt>Updated at</dt><dd>{displayValue(srs.updatedAt)}</dd></div>
                  </>}
                  <div className="srs-metadata-item"><dt>SRS status</dt><dd className={`srs-status ${reviewed ? 'srs-status-reviewed' : 'srs-status-pending'}`}>{status.replace(/_/g, ' ')}</dd></div>
                  <div className="srs-metadata-item"><dt>Functional requirements</dt><dd>{srs ? displayValue(srs.functionalRequirementCount) : '—'}</dd></div>
                  <div className="srs-metadata-item"><dt>Non-functional requirements</dt><dd>{srs ? displayValue(srs.nonFunctionalRequirementCount) : '—'}</dd></div>
                  <div className="srs-metadata-item"><dt>Total requirements</dt><dd>{srs ? displayValue(srs.totalRequirements) : '—'}</dd></div>
                </dl>
                <div className="srs-actions">
                  {analyst && <button type="button" className="srs-generate-button primary-button" onClick={handleGenerate} disabled={Boolean(busy) || reviewed || Boolean(srs) || Boolean(loadNotice)}>
                    {busy === 'generate' ? 'Generating SRS…' : reviewed ? 'SRS Reviewed' : srs ? 'SRS Generated' : 'Generate SRS'}
                  </button>}
                  <button type="button" className="srs-reload-button secondary-button" onClick={handleReload} disabled={Boolean(busy)}>
                    {busy === 'reload' ? 'Loading SRS…' : 'Reload Saved SRS'}
                  </button>
                </div>
                {busy && <p className="srs-busy-message" role="status">{busy === 'generate' ? 'Generating the specification. This may take a moment.' : busy === 'review' ? 'Saving the Business Analyst review…' : 'Loading the saved specification…'}</p>}
              </section>
              {!srs ? (
                <section className="srs-empty review-empty-card">
                  <h2 className="srs-empty-title">{loadNotice ? 'No SRS is loaded' : 'SRS not yet available'}</h2>
                  <p className="srs-empty-description">{loadNotice ? 'Reload the saved SRS to check whether a document already exists.' : 'The stakeholder must finalize the requirements first. Then select Generate SRS to prepare the document for review.'}</p>
                </section>
              ) : viewDocument ? (
                <>
                  <article className="srs-document" aria-label="Software Requirements Specification">
                    <section className="srs-section" aria-labelledby="srs-introduction-title">
                      <h2 id="srs-introduction-title" className="srs-section-title">1. Introduction</h2>
                      {renderParagraphs(srs.introduction)}
                      <h3>1.1 Purpose</h3>
                      {renderParagraphs(srs.purpose)}
                      <h3>1.2 Scope</h3>
                      {renderParagraphs(srs.scope)}
                      <h3>1.3 System Overview</h3>
                      {renderParagraphs(srs.systemOverview)}
                      <h3>1.4 User Roles</h3>
                      {renderRequirements(srs.userRoleRequirements)}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-functional-title">
                      <h2 id="srs-functional-title" className="srs-section-title">2. Functional Requirements ({functional.length})</h2>
                      {renderRequirements(srs.functionalRequirements, 'FR')}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-nonfunctional-title">
                      <h2 id="srs-nonfunctional-title" className="srs-section-title">3. Non-Functional Requirements ({nonFunctional.length})</h2>
                      {renderRequirements(srs.nonFunctionalRequirements, 'NFR')}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-dataRequirements-title">
                      <h2 id="srs-dataRequirements-title" className="srs-section-title">4. Data Requirements</h2>
                      {renderRequirements(srs.dataRequirements)}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-securityRequirements-title">
                      <h2 id="srs-securityRequirements-title" className="srs-section-title">5. Security Requirements</h2>
                      {renderRequirements(srs.securityRequirements)}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-performanceRequirements-title">
                      <h2 id="srs-performanceRequirements-title" className="srs-section-title">6. Performance Requirements</h2>
                      {renderRequirements(srs.performanceRequirements)}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-notificationRequirements-title">
                      <h2 id="srs-notificationRequirements-title" className="srs-section-title">7. Notification Requirements</h2>
                      {renderRequirements(srs.notificationRequirements)}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-errorHandlingRequirements-title">
                      <h2 id="srs-errorHandlingRequirements-title" className="srs-section-title">8. Error Handling Requirements</h2>
                      {renderRequirements(srs.errorHandlingRequirements)}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-coverage-title">
                      <h2 id="srs-coverage-title" className="srs-section-title">9. Requirement Coverage Summary</h2>
                      {coverage.length ? <dl className="srs-metadata">
                        {coverage.map((item, index) => <div className="srs-metadata-item" key={index}>
                          <dt>{displayValue(item?.area)}</dt>
                          <dd>{displayValue(item?.status)}</dd>
                        </div>)}
                      </dl> : <p className="srs-section-empty">{unspecified}</p>}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-validation-title">
                      <h2 id="srs-validation-title" className="srs-section-title">10. Requirement Validation Summary</h2>
                      <dl className="srs-metadata">
                        {[
                          ['Total requirements', srs.totalRequirements],
                          ['Functional requirements', srs.functionalRequirementCount],
                          ['Non-functional requirements', srs.nonFunctionalRequirementCount],
                          ['Stakeholder validated', srs.validatedRequirementCount],
                          ['Ambiguities remaining', ambiguityCount],
                          ['Missing information items', missingInformationCount],
                        ].map(([label, value]) => <div className="srs-metadata-item" key={label}>
                          <dt>{label}</dt><dd>{displayValue(value)}</dd>
                        </div>)}
                      </dl>
                      <h3>Ambiguities</h3>
                      {Array.isArray(srs.ambiguities) && srs.ambiguities.length === 0
                        ? <p className="srs-paragraph">No ambiguities reported.</p>
                        : renderRequirements(requirementItems(srs.ambiguities).map((item) => typeof item === 'string' ? item : item?.issue || requirementText(item)))}
                      <h3>Missing Information</h3>
                      {Array.isArray(srs.missingInformation) && srs.missingInformation.length === 0
                        ? <p className="srs-paragraph">No missing information reported.</p>
                        : renderRequirements(srs.missingInformation)}
                    </section>
                    <section className="srs-section" aria-labelledby="srs-conclusion-title">
                      <h2 id="srs-conclusion-title" className="srs-section-title">11. Conclusion</h2>
                      {renderParagraphs(srs.conclusion)}
                    </section>
                  </article>
                  {analyst && <section className="srs-review-section">
                    <h2 className="srs-review-title">12. Business Analyst Review</h2>
                    <p className="srs-paragraph">Status: <strong>{displayValue(srs.status)}</strong></p>
                    <p className="srs-review-description">{reviewed ? 'This SRS has been reviewed by the Business Analyst.' : 'Check all SRS sections, coverage, and validation summary before marking this specification as reviewed.'}</p>
                    {reviewed ? <p className="srs-reviewed-state" role="status">✓ Business Analyst Review Complete — REVIEWED</p> : <button type="button" className="srs-review-button primary-button" onClick={handleReview} disabled={Boolean(busy)}>{busy === 'review' ? 'Saving Review…' : 'Mark as Reviewed'}</button>}
                  </section>}
                </>
              ) : <section className="review-empty-card"><p>SRS Generated</p><Link className="primary-button" to={`/generated-srs/${encodeURIComponent(interviewId)}`}>View SRS</Link></section>}
            </>
          )}
        </>
      )}
    </main>
  )
}
// ======================================================
// Application
// ======================================================
const AUTH_USER_KEY = 'reAssistantAuthUser'
const ROLES = ['STAKEHOLDER', 'BUSINESS_ANALYST']
function validUser(user) {
  return user && typeof user.id === 'string' && user.id &&
    typeof user.name === 'string' && typeof user.email === 'string' && ROLES.includes(user.role)
}
function readStoredUser() {
  try {
    const user = JSON.parse(localStorage.getItem(AUTH_USER_KEY))
    return validUser(user) ? user : null
  } catch { return null }
}
function AuthPage({ register = false, onAuthenticated }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('STAKEHOLDER')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [assignmentCode, setAssignmentCode] = useState('')
  const needsBusinessAnalyst = register && role === 'STAKEHOLDER'
  async function submit(event) {
    event.preventDefault()
    if (busy) return
    if (needsBusinessAnalyst && !assignmentCode.trim()) {
      setError('Business Analyst assignment code is required.')
      return
    }
    setBusy(true)
    setError('')
    try {
      const result = register
        ? await registerUser(name.trim(), email.trim(), password, role,
            needsBusinessAnalyst ? assignmentCode.trim().toUpperCase() : null)
        : await loginUser(email.trim(), password)
      if (!validUser(result)) throw new Error('The server returned an invalid user or unsupported role.')
      if (register) {
        navigate('/login', { replace: true, state: {
          registrationSuccess: 'Registration successful. Please log in with your email and password.',
        } })
        return
      }
      // Persist only AuthResponse fields, never the submitted password.
      const user = { id: result.id, name: result.name, email: result.email,
        role: result.role, assignedBusinessAnalystId: result.assignedBusinessAnalystId,
        assignmentCode: result.assignmentCode, message: result.message }
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
      onAuthenticated(user)
      navigate('/', { replace: true })
    } catch (cause) {
      setError(cause.message || 'Unable to sign in. Please try again.')
    } finally { setBusy(false) }
  }
  return <main className="auth-page">
    <section className="auth-card">
      <div className="badge">RE Assistant</div>
      <h1>{register ? 'Create your account' : 'Welcome back'}</h1>
      <p>Sign in to your requirements workspace.</p>
      {!register && location.state?.registrationSuccess && <div className="srs-success" role="status">{location.state.registrationSuccess}</div>}
      {error && <div className="workspace-error" role="alert">{error}</div>}
      <form className="auth-form" onSubmit={submit} aria-busy={busy}>
        <fieldset disabled={busy}>
          {register && <label>Name<input required value={name} onChange={e => setName(e.target.value)} autoComplete="name" /></label>}
          <label>Email<input required type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="username" /></label>
          <label>Password<input required type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete={register ? 'new-password' : 'current-password'} /></label>
          {register && <label>Role<select value={role} onChange={e => setRole(e.target.value)}>
            <option value="STAKEHOLDER">Stakeholder</option>
            <option value="BUSINESS_ANALYST">Business Analyst</option>
          </select></label>}
          {needsBusinessAnalyst && <>
            <label htmlFor="registration-assignment-code">Business Analyst Assignment Code
              <input id="registration-assignment-code" required type="text"
                value={assignmentCode}
                onChange={event => setAssignmentCode(event.target.value)}
                placeholder="BA-X7K29P" autoComplete="off" autoCapitalize="characters"
                spellCheck={false} aria-describedby="registration-assignment-code-help" />
            </label>
            <p id="registration-assignment-code-help">
              Enter the assignment code provided by your Business Analyst.
            </p>
          </>}
          <button className="primary-button" type="submit" disabled={needsBusinessAnalyst && !assignmentCode.trim()}>{busy ? 'Please wait…' : register ? 'Register' : 'Log in'}</button>
        </fieldset>
      </form>
      <p>{register ? 'Already have an account?' : 'New here?'} <Link to={register ? '/login' : '/register'}>{register ? 'Log in' : 'Register'}</Link></p>
    </section>
  </main>
}
function App() {
  const [user, setUser] = useState(readStoredUser)
  const [activeInterviewId, setActiveInterviewId] = useState(
    () => { const account = readStoredUser(); return account ? localStorage.getItem(`${ACTIVE_INTERVIEW_KEY}:${account.role}:${account.id}`) || '' : '' },
  )
  // Keep completion visible when navigating away and back during this session.
  const [finalizedInterviewIds, setFinalizedInterviewIds] = useState([])
  function selectInterview(id) {
    const nextId = id ? String(id) : ''
    const key = `${ACTIVE_INTERVIEW_KEY}:${user.role}:${user.id}`
    if (nextId) localStorage.setItem(key, nextId)
    else localStorage.removeItem(key)
    setActiveInterviewId(nextId)
  }
  function markFinalized(id) {
    saveWorkflow(user, id, { finalized: true, reviewEntered: true })
    setFinalizedInterviewIds(current => current.includes(id) ? current : [...current, id])
  }
  function requirementsChanged(id) {
    saveWorkflow(user, id, { finalized: false, reviewEntered: false })
    setFinalizedInterviewIds(current => current.filter(value => value !== id))
  }
  useEffect(() => {
    const key = user ? `${ACTIVE_INTERVIEW_KEY}:${user.role}:${user.id}` : null
    setActiveInterviewId(key ? localStorage.getItem(key) || '' : '')
    setFinalizedInterviewIds([])
    function sync(event) {
      if (event.key === AUTH_USER_KEY || event.key === null) setUser(readStoredUser())
      if (event.key === key || event.key === null) setActiveInterviewId(key ? localStorage.getItem(key) || '' : '')
    }
    window.addEventListener('storage', sync)
    return () => window.removeEventListener('storage', sync)
  }, [user?.id, user?.role])
  const stakeholder = user?.role === 'STAKEHOLDER'
  function logout() {
    localStorage.removeItem(AUTH_USER_KEY)
    setActiveInterviewId('')
    setFinalizedInterviewIds([])
    setUser(null)
  }
  function protectedPage(page, requiredRole = null) {
    if (!user) return <Navigate to="/login" replace />
    if (requiredRole && user.role !== requiredRole) return <Navigate to="/" replace />
    return <Fragment key={`${user.role}:${user.id}`}>{page}</Fragment>
  }
  return <BrowserRouter>
    <div className="app">
      <header className="navbar">
        <Link to="/" className="brand">
          <div className="brand-mark">RE</div>
          <div><strong>RE Assistant</strong><small>Requirements Engineering</small></div>
        </Link>
        <nav aria-label="Main navigation">
          {user ? <>
            <Link to="/">Dashboard</Link>
            {stakeholder && <><Link to="/interview">Interview</Link><Link to="/requirements">Requirements</Link></>}
            {!stakeholder && <><Link to="/srs">SRS</Link><Link to="/generated-srs">Generated SRS</Link></>}
            <span className="auth-user">{user.name} · {stakeholder ? 'Stakeholder' : 'Business Analyst'}</span>
            <button type="button" className="secondary-button" onClick={logout}>Logout</button>
          </> : <><Link to="/login">Log in</Link><Link to="/register">Register</Link></>}
        </nav>
      </header>
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/" replace /> : <AuthPage key="login" onAuthenticated={setUser} />} />
        <Route path="/register" element={user ? <Navigate to="/" replace /> : <AuthPage key="register" register onAuthenticated={setUser} />} />
        <Route path="/" element={protectedPage(<Home user={user} onSelectInterview={selectInterview} />)} />
        <Route path="/interview" element={protectedPage(<Interview key={`${user?.id}:${activeInterviewId}`} user={user} activeInterviewId={activeInterviewId} onSelectInterview={selectInterview} onRequirementsChanged={requirementsChanged} />, 'STAKEHOLDER')} />
        <Route path="/interview/:id" element={protectedPage(<ProjectPage user={user} kind="interview" onSelectInterview={selectInterview} onRequirementsChanged={requirementsChanged} />, 'STAKEHOLDER')} />
        <Route path="/requirements" element={protectedPage(<ProjectList user={user} kind="requirements" />, 'STAKEHOLDER')} />
        <Route path="/requirements/:id" element={protectedPage(<ProjectPage key={`${user?.id}:${finalizedInterviewIds.join(',')}`} user={user} kind="requirements" onFinalized={markFinalized} />, 'STAKEHOLDER')} />
        <Route path="/srs" element={protectedPage(<ProjectList user={user} kind="generation" />, 'BUSINESS_ANALYST')} />
        <Route path="/srs/:id" element={protectedPage(<ProjectPage user={user} kind="generation" />, 'BUSINESS_ANALYST')} />
        <Route path="/generated-srs" element={protectedPage(<ProjectList user={user} kind="generated" />, 'BUSINESS_ANALYST')} />
        <Route path="/generated-srs/:id" element={protectedPage(<ProjectPage user={user} kind="generated" />, 'BUSINESS_ANALYST')} />
        <Route path="*" element={<Navigate to={user ? '/' : '/login'} replace />} />
      </Routes>
    </div>
  </BrowserRouter>
}
export default App
