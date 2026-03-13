import React, { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useAuth, api } from './App';

// ═══════════════════════════════════════════════════════════════════════════════
// SHARED HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

const Badge = ({ val }) => {
  const map = {
    PENDING:'badge-pending', APPROVED:'badge-approved', REJECTED:'badge-rejected',
    CANCELLED:'badge-cancelled', ACTIVE:'badge-active', INACTIVE:'badge-inactive',
    ANNUAL:'badge-annual', SICK:'badge-sick', CASUAL:'badge-casual'
  };
  return <span className={`badge ${map[val]||'badge-pending'}`}>{val}</span>;
};

const Modal = ({ title, onClose, children }) => (
  <div className="modal-overlay" onClick={e => e.target===e.currentTarget && onClose()}>
    <div className="modal">
      <div className="modal-header">
        <h3>{title}</h3>
        <button className="modal-close" onClick={onClose}>×</button>
      </div>
      {children}
    </div>
  </div>
);

// ═══════════════════════════════════════════════════════════════════════════════
// SIDEBAR
// ═══════════════════════════════════════════════════════════════════════════════

export const Sidebar = () => {
  const { user, logout, isAdmin, isManager } = useAuth();
  const navigate = useNavigate();
  const initials = user ? `${user.firstName[0]}${user.lastName[0]}` : 'U';
  const role = user?.roles?.[0]?.replace('ROLE_','') || 'USER';

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <h2>SmartOps</h2>
        <span>Workflow Automation</span>
      </div>
      <nav className="sidebar-nav">
        <div className="nav-group-label">Overview</div>
        <NavLink to="/dashboard" className={({isActive})=>`nav-item${isActive?' active':''}`}>Dashboard</NavLink>

        <div className="nav-group-label">Leave</div>
        <NavLink to="/leaves/my"    className={({isActive})=>`nav-item${isActive?' active':''}`}>My Leaves</NavLink>
        <NavLink to="/leaves/apply" className={({isActive})=>`nav-item${isActive?' active':''}`}>Apply Leave</NavLink>

        {(isAdmin()||isManager()) && <>
          <div className="nav-group-label">Management</div>
          <NavLink to="/leaves/team"    className={({isActive})=>`nav-item${isActive?' active':''}`}>Team Leaves</NavLink>
          <NavLink to="/leaves/pending" className={({isActive})=>`nav-item${isActive?' active':''}`}>Pending Approvals</NavLink>
        </>}

        {isAdmin() && <>
          <div className="nav-group-label">Admin</div>
          <NavLink to="/employees"          className={({isActive})=>`nav-item${isActive?' active':''}`}>Employees</NavLink>
          <NavLink to="/employees/register" className={({isActive})=>`nav-item${isActive?' active':''}`}>Register Employee</NavLink>
          <NavLink to="/leaves/all"         className={({isActive})=>`nav-item${isActive?' active':''}`}>All Leaves</NavLink>
        </>}
      </nav>
      <div className="sidebar-footer">
        <div className="user-info">
          <div className="avatar">{initials}</div>
          <div className="user-info-text">
            <div className="name">{user?.firstName} {user?.lastName}</div>
            <div className="role">{role}</div>
          </div>
        </div>
        <button className="logout-btn" onClick={()=>{logout();navigate('/login');}}>Sign Out</button>
      </div>
    </aside>
  );
};

// ═══════════════════════════════════════════════════════════════════════════════
// DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════════

export const Dashboard = () => {
  const { user, isAdmin, isManager } = useAuth();
  const [stats, setStats] = useState(null);

  useEffect(() => { api.stats().then(r => setStats(r.data)); }, []);

  const canManage = isAdmin() || isManager();
  const Stat = ({ label, val, color }) => (
    <div className={`stat-card ${color}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{val ?? 0}</div>
    </div>
  );

  return (
    <div>
      <div className="page-header">
        <h1>Welcome, {user?.firstName} 👋</h1>
        <p>Your organisation's activity at a glance.</p>
      </div>
      <div className="stats-grid">
        {canManage && <><Stat label="Total Employees"  val={stats?.totalEmployees}  color="blue"/>
                         <Stat label="Active Employees" val={stats?.activeEmployees} color="green"/></>}
        <Stat label="Pending Leaves"      val={stats?.pendingLeaves}  color="yellow"/>
        <Stat label="Approved Leaves"     val={stats?.approvedLeaves} color="green"/>
        <Stat label="Rejected Leaves"     val={stats?.rejectedLeaves} color="red"/>
        <Stat label="My Pending"          val={stats?.myPendingLeaves} color="yellow"/>
        <Stat label="My Approved (YTD)"   val={stats?.myApprovedDays}  color="green"/>
      </div>
      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:20}}>
        <div className="card">
          <h3 style={{fontSize:15,fontWeight:700,marginBottom:16}}>Quick Actions</h3>
          <div style={{display:'flex',flexDirection:'column',gap:10}}>
            <a href="/leaves/apply" className="btn btn-primary" style={{justifyContent:'center'}}>+ Apply for Leave</a>
            <a href="/leaves/my"    className="btn btn-secondary">View My Leave History</a>
            {canManage && <a href="/leaves/pending" className="btn btn-secondary">Review Pending Approvals</a>}
            {isAdmin()  && <a href="/employees/register" className="btn btn-secondary">Register New Employee</a>}
          </div>
        </div>
        <div className="card">
          <h3 style={{fontSize:15,fontWeight:700,marginBottom:16}}>Your Profile</h3>
          {[['Employee ID',user?.employeeId],['Email',user?.email],['Role',user?.roles?.[0]?.replace('ROLE_','')]].map(([l,v])=>(
            <div key={l} style={{display:'flex',justifyContent:'space-between',borderBottom:'1px solid var(--border)',paddingBottom:8,marginBottom:8}}>
              <span style={{fontSize:13,color:'var(--text-muted)'}}>{l}</span>
              <span style={{fontSize:13,fontWeight:600}}>{v}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════════
// EMPLOYEE PAGES
// ═══════════════════════════════════════════════════════════════════════════════

export const EmployeeList = () => {
  const [employees, setEmployees] = useState([]);
  const [search, setSearch] = useState('');
  const [editEmp, setEditEmp] = useState(null);

  const load = () => api.getEmployees().then(r => setEmployees(r.data));
  useEffect(() => { load(); }, []);

  const filtered = employees.filter(e =>
    e.fullName.toLowerCase().includes(search.toLowerCase()) ||
    e.email.toLowerCase().includes(search.toLowerCase()) ||
    e.employeeId.toLowerCase().includes(search.toLowerCase())
  );

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Delete ${name}?`)) return;
    try { await api.deleteEmployee(id); toast.success('Deleted'); load(); }
    catch(e) { toast.error(e.response?.data?.message||'Failed'); }
  };

  return (
    <div>
      <div className="page-header"><h1>Employees</h1></div>
      <div className="topbar">
        <input className="form-input" style={{maxWidth:300}} placeholder="Search name, email, ID..."
          value={search} onChange={e=>setSearch(e.target.value)}/>
        <a href="/employees/register" className="btn btn-primary">+ Add Employee</a>
      </div>
      <div className="card">
        <div className="table-wrap">
          <table>
            <thead><tr><th>Employee</th><th>ID</th><th>Department</th><th>Role</th><th>Status</th><th>Manager</th><th>Actions</th></tr></thead>
            <tbody>
              {filtered.map(emp=>(
                <tr key={emp.id}>
                  <td>
                    <div style={{display:'flex',alignItems:'center',gap:10}}>
                      <div className="avatar" style={{width:30,height:30,fontSize:11}}>{emp.fullName?.split(' ').map(n=>n[0]).join('')}</div>
                      <div><div style={{fontWeight:600,fontSize:13}}>{emp.fullName}</div>
                           <div style={{fontSize:11,color:'var(--text-muted)'}}>{emp.email}</div></div>
                    </div>
                  </td>
                  <td style={{fontFamily:'monospace',fontSize:12,color:'var(--text-muted)'}}>{emp.employeeId}</td>
                  <td>{emp.department||'—'}</td>
                  <td><Badge val={emp.roles?.[0]?.replace('ROLE_','')||'EMPLOYEE'}/></td>
                  <td><Badge val={emp.status}/></td>
                  <td style={{fontSize:13,color:'var(--text-muted)'}}>{emp.managerName||'—'}</td>
                  <td>
                    <div style={{display:'flex',gap:6}}>
                      <button className="btn btn-secondary btn-sm" onClick={()=>setEditEmp(emp)}>Edit</button>
                      <button className="btn btn-danger btn-sm" onClick={()=>handleDelete(emp.id,emp.fullName)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      {editEmp && <EditModal emp={editEmp} employees={employees} onClose={()=>setEditEmp(null)} onSaved={()=>{setEditEmp(null);load();}}/>}
    </div>
  );
};

const EditModal = ({ emp, employees, onClose, onSaved }) => {
  const [form, setForm] = useState({ firstName:emp.fullName?.split(' ')[0]||'', lastName:emp.fullName?.split(' ')[1]||'',
    phone:emp.phone||'', department:emp.department||'', designation:emp.designation||'',
    status:emp.status||'ACTIVE', managerId:emp.managerId||'' });
  const [loading, setLoading] = useState(false);

  const submit = async e => {
    e.preventDefault(); setLoading(true);
    try { await api.updateEmployee(emp.id,{...form,managerId:form.managerId||null}); toast.success('Updated'); onSaved(); }
    catch(err) { toast.error(err.response?.data?.message||'Failed'); }
    finally { setLoading(false); }
  };

  return (
    <Modal title={`Edit — ${emp.fullName}`} onClose={onClose}>
      <form onSubmit={submit}>
        <div className="form-grid-2">
          <div className="form-group"><label className="form-label">First Name</label>
            <input className="form-input" value={form.firstName} onChange={e=>setForm({...form,firstName:e.target.value})} required/></div>
          <div className="form-group"><label className="form-label">Last Name</label>
            <input className="form-input" value={form.lastName} onChange={e=>setForm({...form,lastName:e.target.value})} required/></div>
        </div>
        <div className="form-grid-2">
          <div className="form-group"><label className="form-label">Department</label>
            <input className="form-input" value={form.department} onChange={e=>setForm({...form,department:e.target.value})}/></div>
          <div className="form-group"><label className="form-label">Designation</label>
            <input className="form-input" value={form.designation} onChange={e=>setForm({...form,designation:e.target.value})}/></div>
        </div>
        <div className="form-grid-2">
          <div className="form-group"><label className="form-label">Status</label>
            <select className="form-select" value={form.status} onChange={e=>setForm({...form,status:e.target.value})}>
              <option>ACTIVE</option><option>INACTIVE</option><option>SUSPENDED</option></select></div>
          <div className="form-group"><label className="form-label">Manager</label>
            <select className="form-select" value={form.managerId} onChange={e=>setForm({...form,managerId:e.target.value})}>
              <option value="">No Manager</option>
              {employees.filter(e=>e.id!==emp.id).map(m=><option key={m.id} value={m.id}>{m.fullName}</option>)}
            </select></div>
        </div>
        <div className="modal-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>{loading?'Saving...':'Save'}</button>
        </div>
      </form>
    </Modal>
  );
};

export const RegisterEmployee = () => {
  const navigate = useNavigate();
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({employeeId:'',firstName:'',lastName:'',email:'',password:'',
    phone:'',department:'',designation:'',role:'EMPLOYEE',managerId:''});
  const [loading, setLoading] = useState(false);

  useEffect(()=>{ api.getEmployees().then(r=>setEmployees(r.data)); },[]);
  const s = (k,v) => setForm(f=>({...f,[k]:v}));

  const submit = async e => {
    e.preventDefault(); setLoading(true);
    try { await api.register({...form,managerId:form.managerId||null}); toast.success('Registered!'); navigate('/employees'); }
    catch(err) { toast.error(err.response?.data?.message||'Failed'); }
    finally { setLoading(false); }
  };

  return (
    <div>
      <div className="page-header"><h1>Register Employee</h1></div>
      <div className="card" style={{maxWidth:600}}>
        <form onSubmit={submit}>
          <div className="form-grid-2">
            <div className="form-group"><label className="form-label">Employee ID *</label>
              <input className="form-input" placeholder="EMP002" value={form.employeeId} onChange={e=>s('employeeId',e.target.value)} required/></div>
            <div className="form-group"><label className="form-label">Role</label>
              <select className="form-select" value={form.role} onChange={e=>s('role',e.target.value)}>
                <option value="EMPLOYEE">Employee</option><option value="MANAGER">Manager</option><option value="ADMIN">Admin</option></select></div>
          </div>
          <div className="form-grid-2">
            <div className="form-group"><label className="form-label">First Name *</label>
              <input className="form-input" value={form.firstName} onChange={e=>s('firstName',e.target.value)} required/></div>
            <div className="form-group"><label className="form-label">Last Name *</label>
              <input className="form-input" value={form.lastName} onChange={e=>s('lastName',e.target.value)} required/></div>
          </div>
          <div className="form-group"><label className="form-label">Email *</label>
            <input className="form-input" type="email" value={form.email} onChange={e=>s('email',e.target.value)} required/></div>
          <div className="form-group"><label className="form-label">Password *</label>
            <input className="form-input" type="password" value={form.password} onChange={e=>s('password',e.target.value)} required minLength={8}/></div>
          <div className="form-grid-2">
            <div className="form-group"><label className="form-label">Department</label>
              <input className="form-input" value={form.department} onChange={e=>s('department',e.target.value)}/></div>
            <div className="form-group"><label className="form-label">Designation</label>
              <input className="form-input" value={form.designation} onChange={e=>s('designation',e.target.value)}/></div>
          </div>
          <div className="form-group"><label className="form-label">Manager</label>
            <select className="form-select" value={form.managerId} onChange={e=>s('managerId',e.target.value)}>
              <option value="">No Manager</option>
              {employees.map(m=><option key={m.id} value={m.id}>{m.fullName} ({m.employeeId})</option>)}
            </select></div>
          <div style={{display:'flex',gap:10,marginTop:8}}>
            <button type="button" className="btn btn-secondary" onClick={()=>navigate('/employees')}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>{loading?'Registering...':'Register'}</button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════════
// LEAVE PAGES
// ═══════════════════════════════════════════════════════════════════════════════

export const ApplyLeave = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({leaveType:'ANNUAL',startDate:'',endDate:'',reason:''});
  const [loading, setLoading] = useState(false);
  const s = (k,v) => setForm(f=>({...f,[k]:v}));

  const submit = async e => {
    e.preventDefault(); setLoading(true);
    try { await api.applyLeave(form); toast.success('Leave applied!'); navigate('/leaves/my'); }
    catch(err) { toast.error(err.response?.data?.message||'Failed'); }
    finally { setLoading(false); }
  };

  return (
    <div>
      <div className="page-header"><h1>Apply for Leave</h1></div>
      <div className="card" style={{maxWidth:520}}>
        <form onSubmit={submit}>
          <div className="form-group"><label className="form-label">Leave Type</label>
            <select className="form-select" value={form.leaveType} onChange={e=>s('leaveType',e.target.value)}>
              {['ANNUAL','SICK','CASUAL','MATERNITY','PATERNITY','UNPAID','COMPENSATORY'].map(t=><option key={t}>{t}</option>)}
            </select></div>
          <div className="form-grid-2">
            <div className="form-group"><label className="form-label">Start Date</label>
              <input className="form-input" type="date" value={form.startDate} onChange={e=>s('startDate',e.target.value)} required/></div>
            <div className="form-group"><label className="form-label">End Date</label>
              <input className="form-input" type="date" value={form.endDate} onChange={e=>s('endDate',e.target.value)} required/></div>
          </div>
          <div className="form-group"><label className="form-label">Reason</label>
            <textarea className="form-textarea" value={form.reason} onChange={e=>s('reason',e.target.value)} required placeholder="Describe the reason..."/></div>
          <button type="submit" className="btn btn-primary" disabled={loading} style={{width:'100%',justifyContent:'center'}}>
            {loading?'Submitting...':'Submit Application'}
          </button>
        </form>
      </div>
    </div>
  );
};

const LeaveTable = ({ leaves, showEmployee, onReview, onCancel }) => (
  <div className="table-wrap">
    <table>
      <thead><tr>
        {showEmployee && <th>Employee</th>}
        <th>Type</th><th>From</th><th>To</th><th>Days</th><th>Status</th><th>Actions</th>
      </tr></thead>
      <tbody>
        {leaves.length===0 ? <tr><td colSpan={7} style={{textAlign:'center',padding:40,color:'var(--text-muted)'}}>No records found</td></tr>
        : leaves.map(lr=>(
          <tr key={lr.id}>
            {showEmployee && <td><div style={{fontSize:13,fontWeight:600}}>{lr.employeeName}</div>
              <div style={{fontSize:11,color:'var(--text-muted)'}}>{lr.department}</div></td>}
            <td><Badge val={lr.leaveType}/></td>
            <td style={{fontSize:13}}>{lr.startDate}</td>
            <td style={{fontSize:13}}>{lr.endDate}</td>
            <td style={{fontWeight:600}}>{lr.totalDays}</td>
            <td><Badge val={lr.status}/></td>
            <td>
              <div style={{display:'flex',gap:6}}>
                {onReview && lr.status==='PENDING' && <>
                  <button className="btn btn-success btn-sm" onClick={()=>onReview(lr.id,'APPROVED')}>Approve</button>
                  <button className="btn btn-danger btn-sm"  onClick={()=>onReview(lr.id,'REJECTED')}>Reject</button>
                </>}
                {onCancel && lr.status==='PENDING' &&
                  <button className="btn btn-secondary btn-sm" onClick={()=>onCancel(lr.id)}>Cancel</button>}
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

export const MyLeaves = () => {
  const [leaves, setLeaves] = useState([]);
  const load = () => api.myLeaves().then(r=>setLeaves(r.data));
  useEffect(()=>{load();},[]);
  const cancel = async id => {
    try { await api.cancelLeave(id); toast.success('Cancelled'); load(); }
    catch(e) { toast.error(e.response?.data?.message||'Failed'); }
  };
  return (
    <div>
      <div className="page-header"><h1>My Leaves</h1></div>
      <div className="topbar" style={{justifyContent:'flex-end'}}>
        <a href="/leaves/apply" className="btn btn-primary">+ Apply Leave</a>
      </div>
      <div className="card"><LeaveTable leaves={leaves} onCancel={cancel}/></div>
    </div>
  );
};

export const AllLeaves = () => {
  const [leaves, setLeaves] = useState([]);
  const [filter, setFilter] = useState('');
  useEffect(()=>{ api.allLeaves().then(r=>setLeaves(r.data)); },[]);
  const filtered = filter ? leaves.filter(l=>l.status===filter) : leaves;
  return (
    <div>
      <div className="page-header"><h1>All Leave Requests</h1></div>
      <div className="topbar">
        <select className="form-select" style={{maxWidth:180}} value={filter} onChange={e=>setFilter(e.target.value)}>
          <option value="">All Status</option>
          {['PENDING','APPROVED','REJECTED','CANCELLED'].map(s=><option key={s}>{s}</option>)}
        </select>
      </div>
      <div className="card"><LeaveTable leaves={filtered} showEmployee/></div>
    </div>
  );
};

export const TeamLeaves = () => {
  const [leaves, setLeaves] = useState([]);
  useEffect(()=>{ api.teamLeaves().then(r=>setLeaves(r.data)); },[]);
  return (
    <div>
      <div className="page-header"><h1>Team Leaves</h1></div>
      <div className="card"><LeaveTable leaves={leaves} showEmployee/></div>
    </div>
  );
};

export const PendingApprovals = () => {
  const [leaves, setLeaves] = useState([]);
  const [comment, setComment] = useState('');
  const [modal, setModal] = useState(null);
  const load = () => api.pendingLeaves().then(r=>setLeaves(r.data));
  useEffect(()=>{load();},[]);

  const handleReview = (id, status) => setModal({id, status});
  const confirm = async () => {
    try { await api.reviewLeave(modal.id,{status:modal.status,reviewComments:comment});
      toast.success(modal.status==='APPROVED'?'Approved!':'Rejected'); setModal(null); setComment(''); load(); }
    catch(e) { toast.error(e.response?.data?.message||'Failed'); }
  };

  return (
    <div>
      <div className="page-header"><h1>Pending Approvals</h1><p>{leaves.length} request(s) awaiting review.</p></div>
      <div className="card"><LeaveTable leaves={leaves} showEmployee onReview={handleReview}/></div>
      {modal && (
        <Modal title={`${modal.status==='APPROVED'?'Approve':'Reject'} Leave`} onClose={()=>setModal(null)}>
          <div className="form-group"><label className="form-label">Comments (optional)</label>
            <textarea className="form-textarea" value={comment} onChange={e=>setComment(e.target.value)} placeholder="Add a note..."/></div>
          <div className="modal-footer">
            <button className="btn btn-secondary" onClick={()=>setModal(null)}>Cancel</button>
            <button className={`btn ${modal.status==='APPROVED'?'btn-success':'btn-danger'}`} onClick={confirm}>
              Confirm {modal.status}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════════
// LOGIN PAGE
// ═══════════════════════════════════════════════════════════════════════════════

export const LoginPage = () => {
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({email:'',password:''});
  const [loading, setLoading] = useState(false);

  useEffect(()=>{ if(user) navigate('/dashboard'); },[user]);

  const submit = async e => {
    e.preventDefault(); setLoading(true);
    try { await login(form.email, form.password); navigate('/dashboard'); }
    catch(err) { toast.error(err.response?.data?.message||'Invalid credentials'); }
    finally { setLoading(false); }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo"><h1>SmartOps</h1><p>Employee Workflow Automation</p></div>
        <form onSubmit={submit}>
          <div className="form-group"><label className="form-label">Email</label>
            <input className="form-input" type="email" placeholder="admin@smartops.com"
              value={form.email} onChange={e=>setForm({...form,email:e.target.value})} required/></div>
          <div className="form-group"><label className="form-label">Password</label>
            <input className="form-input" type="password" placeholder="••••••••"
              value={form.password} onChange={e=>setForm({...form,password:e.target.value})} required/></div>
          <button className="btn btn-primary" type="submit" disabled={loading}
            style={{width:'100%',justifyContent:'center',marginTop:8}}>
            {loading?'Signing in...':'Sign In'}
          </button>
        </form>
        <div style={{marginTop:20,padding:12,background:'var(--surface2)',borderRadius:8,fontSize:12,color:'var(--text-muted)'}}>
          <strong style={{color:'var(--text)'}}>Default:</strong> admin@smartops.com / Admin@123
        </div>
      </div>
    </div>
  );
};
