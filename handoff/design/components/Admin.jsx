// SIA Admin — shared components

const { useState: uS, useEffect: uE } = React;

const ADMIN_SECTIONS = [
  { key: 'dashboard',   label: 'Tableau de bord',     icon: 'home',        group: 'Activité' },
  { key: 'orders',      label: 'Commandes',            icon: 'box',         group: 'Activité', badge: '3' },
  { key: 'catalog',     label: 'Catalogue produits',   icon: 'layers',      group: 'Vente' },
  { key: 'inventory',   label: 'Inventaire',           icon: 'barcode',     group: 'Vente' },
  { key: 'promotions',  label: 'Promotions & prix',    icon: 'percent',     group: 'Vente' },
  { key: 'clients',     label: 'Clients',              icon: 'users',       group: 'Relation' },
  { key: 'shipping',    label: 'Expédition',           icon: 'truck',       group: 'Relation' },
  { key: 'content',     label: 'Contenu Home',         icon: 'edit',        group: 'Contenu' },
  { key: 'permissions', label: 'Rôles & permissions',  icon: 'lock',        group: 'Contenu' },
  { key: 'analytics',   label: 'Analytique',           icon: 'chart',       group: 'Pilotage' },
  { key: 'integration', label: 'Supervision intégration', icon: 'trending', group: 'Pilotage' },
  { key: 'settings',    label: 'Paramètres',           icon: 'settings',    group: 'Pilotage' },
];

function AdminSideNav({ active, onChange }) {
  const groups = {};
  ADMIN_SECTIONS.forEach(s => { groups[s.group] = groups[s.group] || []; groups[s.group].push(s); });
  return (
    <aside style={{ width: 240, background: 'var(--ink)', color: 'white', display: 'flex', flexDirection: 'column', position: 'sticky', top: 0, height: '100vh', overflow: 'auto' }}>
      <div style={{ padding: '18px 20px', borderBottom: '1px solid oklch(0.28 0.01 250)', display: 'flex', alignItems: 'center', gap: 10 }}>
        <img src="assets/logo.png" alt="SIA" style={{ width: 36, height: 36, objectFit: 'contain', flexShrink: 0 }} />
        <div>
          <div style={{ fontWeight: 700, letterSpacing: '-0.02em' }}>Console Admin</div>
          <div style={{ fontSize: 10, opacity: 0.6, letterSpacing: '0.08em', textTransform: 'uppercase' }}>SIA B2B v2.4</div>
        </div>
      </div>
      <div style={{ padding: 12, flex: 1 }}>
        {Object.entries(groups).map(([g, items]) => (
          <div key={g} style={{ marginBottom: 14 }}>
            <div style={{ padding: '6px 10px', fontSize: 10, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'oklch(0.55 0.01 250)' }}>{g}</div>
            {items.map(s => (
              <button key={s.key} onClick={() => onChange(s.key)} style={{
                width: '100%', display: 'flex', alignItems: 'center', gap: 10, padding: '9px 10px', borderRadius: 6,
                background: active === s.key ? 'oklch(0.28 0.01 250)' : 'transparent',
                color: active === s.key ? 'white' : 'oklch(0.78 0.01 250)',
                fontSize: 13, fontWeight: 500, marginBottom: 2, textAlign: 'left',
                borderLeft: active === s.key ? '2px solid var(--sia-red)' : '2px solid transparent',
              }}>
                <Icon name={s.icon} size={15} style={{ color: active === s.key ? 'var(--sia-red)' : 'oklch(0.55 0.01 250)' }} />
                <span style={{ flex: 1 }}>{s.label}</span>
                {s.badge && <span style={{ padding: '1px 7px', background: 'var(--sia-red)', color: 'white', borderRadius: 999, fontSize: 10, fontWeight: 700 }}>{s.badge}</span>}
              </button>
            ))}
          </div>
        ))}
      </div>
      <div style={{ padding: 14, borderTop: '1px solid oklch(0.28 0.01 250)', display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ width: 32, height: 32, borderRadius: '50%', background: 'oklch(0.32 0.01 250)', display: 'grid', placeItems: 'center', fontSize: 11, fontWeight: 700 }}>MK</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Mehdi Karim</div>
          <div style={{ fontSize: 10, color: 'oklch(0.60 0.01 250)' }}>Super admin</div>
        </div>
        <a href="Home.html" title="Retour au B2B" style={{ color: 'oklch(0.60 0.01 250)' }}><Icon name="externalLink" size={14} /></a>
      </div>
    </aside>
  );
}

function AdminTopBar({ title, subtitle, actions }) {
  return (
    <div style={{ background: 'var(--surface)', borderBottom: '1px solid var(--border)', padding: '18px 28px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'sticky', top: 0, zIndex: 10 }}>
      <div>
        <h1 style={{ margin: 0, fontSize: 20, fontWeight: 700, letterSpacing: '-0.02em' }}>{title}</h1>
        {subtitle && <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>{subtitle}</div>}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        {actions}
        <button className="icon-btn"><Icon name="bell" size={18} /></button>
        <button className="icon-btn"><Icon name="settings" size={18} /></button>
      </div>
    </div>
  );
}

function Kpi({ label, value, sub, delta, accent }) {
  return (
    <div className="kpi" style={{ padding: 18 }}>
      <div className="kpi-label">{label}</div>
      <div className="kpi-value" style={{ color: accent ? 'var(--sia-red)' : 'var(--ink)', fontSize: 24 }}>{value}</div>
      {sub && <div style={{ fontSize: 12, color: 'var(--ink-3)' }}>{sub}</div>}
      {delta && <div className={`kpi-delta ${delta.up ? 'up' : 'down'}`}><Icon name={delta.up ? 'arrowUp' : 'arrowDown'} size={12} /> {delta.v}</div>}
    </div>
  );
}

// Inline SVG chart — simple line
function LineChart({ data, height = 180, color = 'var(--sia-red)' }) {
  const w = 800; const h = height;
  const max = Math.max(...data);
  const step = w / (data.length - 1);
  const points = data.map((v, i) => `${i * step},${h - (v / max) * (h - 20) - 10}`).join(' ');
  const area = `M 0,${h} L ${points.replace(/(\S+)/g, '$1 L').slice(0, -2)} L ${w},${h} Z`;
  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height }}>
      <defs>
        <linearGradient id="g1" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor={color} stopOpacity="0.18" /><stop offset="100%" stopColor={color} stopOpacity="0" /></linearGradient>
      </defs>
      {[0.25, 0.5, 0.75].map(p => <line key={p} x1="0" y1={h * p} x2={w} y2={h * p} stroke="oklch(0.92 0.005 60)" strokeWidth="1" strokeDasharray="2 4" />)}
      <path d={area} fill="url(#g1)" />
      <polyline points={points} fill="none" stroke={color} strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />
      {data.map((v, i) => <circle key={i} cx={i * step} cy={h - (v / max) * (h - 20) - 10} r="3" fill="white" stroke={color} strokeWidth="2" />)}
    </svg>
  );
}

function BarChart({ data, labels, height = 180, color = 'var(--ink)' }) {
  const w = 800; const h = height;
  const max = Math.max(...data);
  const bw = w / data.length - 8;
  return (
    <svg viewBox={`0 0 ${w} ${h + 24}`} style={{ width: '100%', height: height + 24 }}>
      {data.map((v, i) => {
        const bh = (v / max) * (h - 10);
        return <g key={i}>
          <rect x={i * (bw + 8) + 4} y={h - bh} width={bw} height={bh} rx="3" fill={color} opacity={0.85} />
          <text x={i * (bw + 8) + 4 + bw / 2} y={h + 16} fill="var(--ink-4)" fontSize="10" textAnchor="middle" fontFamily="var(--font-mono)">{labels?.[i]}</text>
        </g>;
      })}
    </svg>
  );
}

Object.assign(window, { ADMIN_SECTIONS, AdminSideNav, AdminTopBar, Kpi, LineChart, BarChart });
