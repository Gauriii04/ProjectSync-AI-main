import { useCallback, useEffect, useState } from 'react'
import {
  LayoutDashboard,
  UserRound,
  Users,
  FileText,
  ClipboardCheck,
  CheckCircle2,
  AlertTriangle,
  OctagonAlert,
  CircleDashed,
} from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import Button from '../components/ui/Button.jsx'
import Badge from '../components/ui/Badge.jsx'
import {
  getMonitoredProjects,
  getProjectAnalytics,
  analyzeSprint,
  getEvaluationReport,
} from '../services/facultyAnalyticsService.js'
import '../styles/monitoring.css'

const facultyNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/faculty/dashboard' },
  { label: 'Faculty Profile', icon: UserRound, path: '/faculty/profile' },
  { label: 'Student Teams', icon: Users, path: null },
  { label: 'Project Proposals', icon: FileText, path: '/faculty/proposals' },
  { label: 'Project Monitoring', icon: ClipboardCheck, path: '/faculty/monitoring' },
]

// Status is always icon + label + color, never color alone.
const healthByRisk = {
  LOW: { label: 'Healthy', tone: 'success', Icon: CheckCircle2 },
  MEDIUM: { label: 'Imbalance', tone: 'warning', Icon: AlertTriangle },
  HIGH: { label: 'Critical / Free-Rider Alert', tone: 'danger', Icon: OctagonAlert },
}
const notAnalyzed = { label: 'Not analyzed yet', tone: 'neutral', Icon: CircleDashed }

const severityTone = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'neutral' }

function ContributionChart({ metrics }) {
  if (metrics.length === 0) {
    return <p className="mon-empty">Run the AI analysis to compute contribution shares.</p>
  }
  const sorted = [...metrics].sort((a, b) => b.contributionPercentage - a.contributionPercentage)
  return (
    <div className="mon-hbars" role="list">
      {sorted.map((m) => (
        <div
          key={m.studentUserId}
          className="mon-hbar-row"
          role="listitem"
          title={`${m.fullName}: ${m.contributionPercentage.toFixed(1)}% · ${m.totalCommits} commits · +${m.linesAdded}/-${m.linesDeleted} lines`}
        >
          <span className="mon-hbar-label">{m.fullName}</span>
          <div className="mon-hbar-track">
            <div className="mon-hbar-fill" style={{ width: `${Math.max(m.contributionPercentage, 0.5)}%` }} />
          </div>
          <span className="mon-hbar-value">{m.contributionPercentage.toFixed(0)}%</span>
        </div>
      ))}
    </div>
  )
}

function VelocityChart({ sprints }) {
  if (sprints.length === 0) {
    return <p className="mon-empty">No sprints yet. The team creates them through the project API.</p>
  }
  const maxPoints = Math.max(1, ...sprints.flatMap((s) => [s.plannedPoints, s.completedPoints]))
  const maxCommits = Math.max(1, ...sprints.map((s) => s.commits))

  return (
    <>
      <div className="mon-legend">
        <span><i className="mon-swatch mon-planned" /> Planned points</span>
        <span><i className="mon-swatch mon-done" /> Completed points</span>
      </div>
      <div className="mon-vbars">
        {sprints.map((s) => (
          <div key={s.id} className="mon-vbar-group">
            <div className="mon-vbar-pair">
              {[
                ['planned', s.plannedPoints, 'Planned'],
                ['done', s.completedPoints, 'Completed'],
              ].map(([kind, value, label]) => (
                <div key={kind} className="mon-vbar-col" title={`${s.name} · ${label}: ${value} pts`}>
                  <span className="mon-vbar-value">{value}</span>
                  <div className={`mon-vbar mon-${kind}`} style={{ height: `${(value / maxPoints) * 100}%` }} />
                </div>
              ))}
            </div>
            <span className="mon-vbar-label">{s.name}</span>
          </div>
        ))}
      </div>

      <h4 className="mon-subhead">Commits per sprint</h4>
      <div className="mon-hbars">
        {sprints.map((s) => (
          <div key={s.id} className="mon-hbar-row" title={`${s.name}: ${s.commits} commits · ${s.codeVolume} lines of code volume`}>
            <span className="mon-hbar-label">{s.name}</span>
            <div className="mon-hbar-track">
              <div className="mon-hbar-fill" style={{ width: `${(s.commits / maxCommits) * 100}%` }} />
            </div>
            <span className="mon-hbar-value">{s.commits}</span>
          </div>
        ))}
      </div>
    </>
  )
}

function FacultyProjectMonitoring() {
  const [projects, setProjects] = useState([])
  const [projectId, setProjectId] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [analyzing, setAnalyzing] = useState(false)
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    getMonitoredProjects()
      .then((response) => {
        setProjects(response.data)
        if (response.data.length > 0) setProjectId(response.data[0].id)
        else setLoading(false)
      })
      .catch(() => {
        setError('Unable to load projects right now.')
        setLoading(false)
      })
  }, [])

  const load = useCallback(async (pid, sprintId) => {
    setLoading(true)
    setError('')
    try {
      const response = await getProjectAnalytics(pid, sprintId)
      setData(response.data)
    } catch {
      setError('Unable to load analytics for this project.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (projectId) load(projectId)
  }, [projectId, load])

  const handleAnalyze = async () => {
    setAnalyzing(true)
    setError('')
    try {
      const response = await analyzeSprint(projectId, data.selectedSprintId)
      setData(response.data)
    } catch {
      setError('Analysis failed. Please try again.')
    } finally {
      setAnalyzing(false)
    }
  }

  const handleExport = async () => {
    // Open the tab synchronously so the popup blocker allows it, then fill it.
    const tab = window.open('', '_blank')
    setExporting(true)
    try {
      const response = await getEvaluationReport(projectId)
      const url = URL.createObjectURL(new Blob([response.data], { type: 'text/html' }))
      if (tab) tab.location.href = url
      else window.location.href = url
    } catch {
      tab?.close()
      setError('Unable to generate the evaluation report.')
    } finally {
      setExporting(false)
    }
  }

  const insight = data?.insight
  const health = insight ? healthByRisk[insight.riskLevel] : notAnalyzed
  const metrics = data?.metrics || []
  const sprints = data?.sprints || []

  return (
    <DashboardLayout navItems={facultyNavItems}>
      <h1 className="content-title">Project Monitoring</h1>
      <p className="content-subtitle">Verified contribution analytics and AI risk alerts for approved projects.</p>

      {projects.length > 0 && (
        <div className="mon-filters">
          <div className="ui-field">
            <label htmlFor="project" className="ui-label">Project</label>
            <select id="project" className="ui-input" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              {projects.map((p) => (
                <option key={p.id} value={p.id}>{p.title} ({p.teamName})</option>
              ))}
            </select>
          </div>
          {sprints.length > 0 && (
            <div className="ui-field">
              <label htmlFor="sprint" className="ui-label">Sprint</label>
              <select
                id="sprint"
                className="ui-input"
                value={data.selectedSprintId || ''}
                onChange={(e) => load(projectId, e.target.value)}
              >
                {sprints.map((s) => (
                  <option key={s.id} value={s.id}>{s.name} ({s.startDate} → {s.endDate})</option>
                ))}
              </select>
            </div>
          )}
        </div>
      )}

      {error && <div className="mon-error">{error}</div>}

      {!loading && projects.length === 0 && !error ? (
        <div className="mon-card"><p className="mon-empty">No approved projects to monitor yet.</p></div>
      ) : loading && !data ? (
        <div className="mon-card"><p className="mon-empty">Loading...</p></div>
      ) : data && (
        <>
          <div className="mon-card mon-header">
            <div>
              <h2>{data.project.title}</h2>
              <p className="mon-meta">Team {data.project.teamName}</p>
            </div>
            <Badge tone={health.tone}>
              <health.Icon size={13} aria-hidden="true" className="mon-badge-icon" />
              {health.label}
            </Badge>
            <div className="mon-actions">
              <Button onClick={handleAnalyze} loading={analyzing} disabled={!data.selectedSprintId}>
                {analyzing ? 'Analyzing...' : 'Run AI analysis'}
              </Button>
              <Button variant="secondary" onClick={handleExport} loading={exporting}>
                Export viva report
              </Button>
            </div>
          </div>

          <div className="mon-grid">
            <div className="mon-card">
              <h3>Individual contribution share</h3>
              <p className="mon-hint">Share of the sprint's code volume (lines added + 0.5 × lines deleted).</p>
              <ContributionChart metrics={metrics} />
            </div>

            <div className="mon-card">
              <h3>Sprint velocity</h3>
              <VelocityChart sprints={sprints} />
            </div>
          </div>

          {metrics.length > 0 && (
            <div className="mon-card">
              <h3>Per-student metrics</h3>
              <div className="mon-table-wrap">
                <table className="mon-table">
                  <thead>
                    <tr>
                      <th>Student</th><th>Commits</th><th>Lines +/−</th><th>Tasks done</th>
                      <th>Consistency</th><th>Share</th>
                    </tr>
                  </thead>
                  <tbody>
                    {metrics.map((m) => (
                      <tr key={m.studentUserId}>
                        <td>{m.fullName}</td>
                        <td>{m.totalCommits}</td>
                        <td>+{m.linesAdded} / −{m.linesDeleted}</td>
                        <td>{m.tasksCompleted}</td>
                        <td>{m.consistencyScore.toFixed(2)}</td>
                        <td>{m.contributionPercentage.toFixed(1)}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {insight && (
            <div className="mon-grid">
              <div className="mon-card">
                <h3>AI executive summary</h3>
                {insight.executiveSummary ? (
                  insight.executiveSummary.split(/\n\s*\n/).map((para, i) => (
                    <p key={i} className="mon-summary">{para}</p>
                  ))
                ) : (
                  <p className="mon-empty">The AI summary was unavailable for this run. Risk flags below are still valid.</p>
                )}
                <p className="mon-meta">Generated {new Date(insight.generatedAt).toLocaleString()}</p>
              </div>

              <div className="mon-card">
                <h3>Risk flags</h3>
                {insight.flags.length === 0 ? (
                  <p className="mon-empty">No anomalies detected.</p>
                ) : (
                  <ul className="mon-flags">
                    {insight.flags.map((f, i) => (
                      <li key={i}>
                        <Badge tone={severityTone[f.severity]}>{f.severity}</Badge>
                        <span>{f.message}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}
        </>
      )}
    </DashboardLayout>
  )
}

export default FacultyProjectMonitoring
