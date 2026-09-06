import React, { useEffect, useState } from 'react'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://topdev-app-prod.tail3d7213.ts.net:8080'

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  })
  const text = await response.text()
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { data = text }
  if (!response.ok) throw new Error(data?.error || `Request failed (HTTP ${response.status})`)
  return data
}

const money = value => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value || 0))

export default function App() {
  const [accounts, setAccounts] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [ownerName, setOwnerName] = useState('')
  const [accountType, setAccountType] = useState('CHECKING')
  const [amount, setAmount] = useState('100')
  const [transactions, setTransactions] = useState([])
  const [sim, setSim] = useState({ transactions: 100, minDelayMs: 10, maxDelayMs: 50 })
  const [run, setRun] = useState({ id: '', status: '' })
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState({ type: 'info', text: 'Connecting to banking API...' })
  const selected = accounts.find(account => account.id === selectedId)
  const total = accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0)

  async function refreshAccounts() {
    const data = await api('/accounts')
    setAccounts(data)
    if (!selectedId && data.length) setSelectedId(data[0].id)
  }

  async function refreshTransactions(id = selectedId) {
    setTransactions(id ? await api(`/transactions?account=${encodeURIComponent(id)}&limit=50`) : [])
  }

  useEffect(() => {
    refreshAccounts().then(() => setNotice({ type: 'success', text: 'Connected to banking API.' }))
      .catch(error => setNotice({ type: 'error', text: error.message }))
  }, [])

  useEffect(() => { refreshTransactions().catch(error => setNotice({ type: 'error', text: error.message })) }, [selectedId])

  useEffect(() => {
    if (!run.id || run.status !== 'RUNNING') return undefined
    const timer = setInterval(async () => {
      try {
        const data = await api(`/simulate/status/${run.id}`)
        setRun({ id: run.id, status: data.status })
        if (data.status !== 'RUNNING') { await refreshAccounts(); await refreshTransactions() }
      } catch (error) { setNotice({ type: 'error', text: error.message }) }
    }, 1000)
    return () => clearInterval(timer)
  }, [run.id, run.status])

  async function createAccount(event) {
    event.preventDefault()
    if (!ownerName.trim()) return setNotice({ type: 'error', text: 'Enter an owner name.' })
    setBusy(true)
    try { await api('/accounts', { method: 'POST', body: JSON.stringify({ ownerName: ownerName.trim(), accountType }) }); setOwnerName(''); await refreshAccounts(); setNotice({ type: 'success', text: 'Account created.' }) }
    catch (error) { setNotice({ type: 'error', text: error.message }) } finally { setBusy(false) }
  }

  async function transact(operation) {
    const value = Number(amount)
    if (!selected) return setNotice({ type: 'error', text: 'Select an account first.' })
    if (!Number.isFinite(value) || value <= 0) return setNotice({ type: 'error', text: 'Enter an amount greater than zero.' })
    setBusy(true)
    try { await api(`/accounts/${selected.id}/${operation}`, { method: 'POST', body: JSON.stringify({ amount: value }) }); await refreshAccounts(); await refreshTransactions(selected.id); setNotice({ type: 'success', text: `${operation === 'deposit' ? 'Deposit' : 'Withdrawal'} completed.` }) }
    catch (error) { setNotice({ type: 'error', text: error.message }) } finally { setBusy(false) }
  }

  async function startSimulation(event) {
    event.preventDefault()
    const config = Object.fromEntries(Object.entries(sim).map(([key, value]) => [key, Number(value)]))
    if (config.transactions < 1 || config.minDelayMs < 0 || config.maxDelayMs < config.minDelayMs) return setNotice({ type: 'error', text: 'Check simulator values.' })
    setBusy(true)
    try { const data = await api('/simulate/start', { method: 'POST', body: JSON.stringify(config) }); setRun({ id: data.runId, status: 'RUNNING' }); setNotice({ type: 'success', text: 'Simulation started. Balances will refresh automatically.' }) }
    catch (error) { setNotice({ type: 'error', text: error.message }) } finally { setBusy(false) }
  }

  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><span className="brand-mark">$</span><span>Northstar <b>Banking</b></span></div><nav><a className="active" href="#overview">Overview</a><a href="#accounts">Accounts</a><a href="#activity">Activity</a><a href="#simulator">Simulator</a></nav><div className="sidebar-footer"><span className="online-dot" /> API connected<small>Local development</small></div></aside>
    <main className="main-content">
      <header className="topbar"><div><p className="eyebrow">PERSONAL BANKING / OVERVIEW</p><h1>Good afternoon.</h1></div><div className="profile"><span className="avatar">D</span><span><b>Demo operator</b><small>Administrator</small></span></div></header>
      <div className={`notice ${notice.type}`} role="status"><b>{notice.type === 'error' ? '!' : '✓'}</b>{notice.text}</div>
      <section id="overview" className="summary-grid"><div className="summary-card highlight"><p>TOTAL BALANCE</p><strong>{money(total)}</strong><span>Across {accounts.length} active account{accounts.length === 1 ? '' : 's'}</span></div><div className="summary-card"><p>ACTIVE ACCOUNTS</p><strong>{accounts.length}</strong><span>Available now</span></div><div className="summary-card"><p>RECENT EVENTS</p><strong>{transactions.length}</strong><span>Selected account</span></div></section>
      <div className="content-grid">
        <section id="accounts" className="panel"><div className="section-heading"><div><p className="eyebrow">YOUR MONEY</p><h2>Accounts</h2></div><button className="text-button" onClick={() => refreshAccounts().catch(error => setNotice({ type: 'error', text: error.message }))}>Refresh</button></div>{accounts.length ? <div className="account-list">{accounts.map(account => <button className={`account-row ${selectedId === account.id ? 'selected' : ''}`} key={account.id} onClick={() => setSelectedId(account.id)}><span className="account-icon">{account.accountType === 'SAVINGS' ? 'S' : 'C'}</span><span><b>{account.ownerName}</b><small>{account.accountType} · {account.id.slice(0, 8)}...</small></span><strong>{money(account.balance)}</strong><i>›</i></button>)}</div> : <div className="empty">No accounts yet. Open one below.</div>}<form className="create-form" onSubmit={createAccount}><h3>Open a new account</h3><label>Owner name<input value={ownerName} onChange={event => setOwnerName(event.target.value)} placeholder="e.g. Alex Morgan" /></label><label>Account type<select value={accountType} onChange={event => setAccountType(event.target.value)}><option>CHECKING</option><option>SAVINGS</option></select></label><button className="primary-button" disabled={busy}>{busy ? 'Working...' : '+ Create account'}</button></form></section>
        <section className="panel action-panel"><div className="section-heading"><div><p className="eyebrow">SELECTED ACCOUNT</p><h2>{selected ? selected.ownerName : 'Choose an account'}</h2></div><span className="badge">{selected?.accountType || 'NONE'}</span></div>{selected ? <><div className="balance">{money(selected.balance)}<small>Available balance</small></div><label className="amount">Transaction amount<input type="number" min="0.01" step="0.01" value={amount} onChange={event => setAmount(event.target.value)} /></label><div className="action-buttons"><button disabled={busy} onClick={() => transact('deposit')}>Deposit</button><button disabled={busy} onClick={() => transact('withdraw')}>Withdraw</button></div><p className="helper">Withdrawals cannot exceed available balance.</p></> : <div className="empty">Select an account to manage its balance.</div>}</section>
      </div>
      <div className="lower-grid"><section id="activity" className="panel"><div className="section-heading"><div><p className="eyebrow">LEDGER</p><h2>Recent activity</h2></div></div>{transactions.length ? <div className="table-wrap"><table><thead><tr><th>Type</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead><tbody>{transactions.slice(0, 8).map(item => <tr key={item.id}><td><b>{item.event_type}</b><small>{item.account_id?.slice(0, 8)}...</small></td><td>{money(item.amount)}</td><td><span className={`status ${item.status === 'SUCCESS' ? 'success' : 'failed'}`}>{item.status}</span></td><td>{item.ts ? new Date(item.ts).toLocaleString() : '-'}</td></tr>)}</tbody></table></div> : <div className="empty">No transactions recorded yet.</div>}</section><section id="simulator" className="panel"><div className="section-heading"><div><p className="eyebrow">LOAD TESTING</p><h2>Simulator</h2></div><span className={`run-status ${run.status === 'RUNNING' ? 'running' : ''}`}>{run.status || 'IDLE'}</span></div><p className="description">Generate realistic activity against your accounts.</p><form className="sim-form" onSubmit={startSimulation}><label>Transactions<input type="number" min="1" value={sim.transactions} onChange={event => setSim({ ...sim, transactions: event.target.value })} /></label><label>Min delay<input type="number" min="0" value={sim.minDelayMs} onChange={event => setSim({ ...sim, minDelayMs: event.target.value })} /></label><label>Max delay<input type="number" min="0" value={sim.maxDelayMs} onChange={event => setSim({ ...sim, maxDelayMs: event.target.value })} /></label><button className="primary-button" disabled={busy || run.status === 'RUNNING'}>{run.status === 'RUNNING' ? 'Simulation running' : 'Start simulation'}</button></form>{run.id && <p className="run-id">Run <code>{run.id.slice(0, 18)}...</code></p>}</section></div>
    </main>
  </div>
}
