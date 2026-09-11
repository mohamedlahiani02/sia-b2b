// SIA B2B — Shared chrome: header, vehicle bar, footer

const { useState, useEffect, useRef } = React;

const SEARCH_SCOPES = [
  { key: 'ref', label: 'Par référence' },
  { key: 'oem', label: 'Par origine OEM' },
  { key: 'ean', label: 'Par code-barre EAN' },
  { key: 'text', label: 'Recherche libre' },
];

function AppHeader({ cartCount = 0, currentPath = '' }) {
  const [scope, setScope] = useState('ref');
  const [query, setQuery] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const [scopeOpen, setScopeOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    const close = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setMenuOpen(false);
        setScopeOpen(false);
      }
    };
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, []);

  const submitSearch = (e) => {
    e?.preventDefault();
    const url = `Search.html${query ? `?q=${encodeURIComponent(query)}` : ''}`;
    window.location.href = url;
  };

  return (
    <header className="app-header">
      <div className="app-header-inner">
        <a href="Home.html" className="brand">
          <img src="assets/logo.png" alt="SIA Logo" style={{ width: 40, height: 40, objectFit: 'contain', flexShrink: 0 }} />
          <div>
            <div className="brand-name" style={{ fontSize: 16, letterSpacing: '-0.015em' }}>SIA B2B</div>
            <div className="brand-sub">بن جماعة وشركاؤه · Sfax</div>
          </div>
        </a>

        <nav style={{ display: 'flex', gap: 2 }}>
          <a href="Home.html" className="icon-btn" title="Véhicules légers" style={{ color: currentPath === 'home' ? 'var(--sia-red)' : undefined }}>
            <Icon name="car" size={20} />
          </a>
          <a href="Search.html" className="icon-btn" title="Poids lourds">
            <Icon name="truck" size={20} />
          </a>
        </nav>

        <form className="search-wrap" onSubmit={submitSearch} ref={menuRef}>
          <button
            type="button"
            className="search-scope"
            onClick={(e) => { e.stopPropagation(); setScopeOpen(!scopeOpen); }}
          >
            {SEARCH_SCOPES.find(s => s.key === scope)?.label}
            <Icon name="chevronDown" size={12} style={{ marginLeft: 6 }} />
          </button>
          {scopeOpen && (
            <div className="menu" style={{ top: 44, left: 0 }}>
              {SEARCH_SCOPES.map(s => (
                <button key={s.key} type="button" onClick={() => { setScope(s.key); setScopeOpen(false); }}>
                  {s.label}
                </button>
              ))}
            </div>
          )}
          <input
            className="search-input"
            placeholder="Rechercher une référence, un OEM, un code-barre…"
            value={query}
            onChange={e => setQuery(e.target.value)}
          />
          <button type="submit" className="search-btn">
            <Icon name="search" size={14} /> Rechercher
          </button>
        </form>

        <div className="header-actions">
          <a href="Account.html" className="icon-btn" title="Notifications">
            <Icon name="bell" size={18} />
            <span className="count">3</span>
          </a>
          <div style={{ position: 'relative' }}>
            <button className="user-chip" onClick={(e) => { e.stopPropagation(); setMenuOpen(!menuOpen); }}>
              <div className="av">EB</div>
              <span style={{ fontWeight: 600 }}>ETS BEN DJEMAA</span>
              <Icon name="chevronDown" size={12} style={{ color: 'var(--ink-3)' }} />
            </button>
            {menuOpen && (
              <div className="menu" style={{ right: 0, top: 44 }}>
                <a href="Account.html"><Icon name="user" size={14} /> Mon compte</a>
                <a href="Account.html?tab=orders"><Icon name="box" size={14} /> Mes commandes</a>
                <a href="Account.html?tab=invoices"><Icon name="file" size={14} /> Mes factures</a>
                <div className="divider"></div>
                <a href="Login.html"><Icon name="lock" size={14} /> Déconnexion</a>
              </div>
            )}
          </div>
          <a href="Cart.html" className="icon-btn" title="Panier">
            <Icon name="cart" size={20} />
            {cartCount > 0 && <span className="count">{cartCount}</span>}
          </a>
        </div>
      </div>
    </header>
  );
}

function VehicleBar({ vehicle, onChange }) {
  const BRANDS = window.SIA_DATA.BRANDS.filter(b => b.length > 1);
  const [brand, setBrand] = useState(vehicle?.brand || '');
  const [model, setModel] = useState(vehicle?.model || '');
  const [version, setVersion] = useState(vehicle?.version || '');

  const models = brand ? (window.SIA_DATA.MODELS_BY_BRAND[brand] || ['100 C3', 'A4', 'Série 3']) : [];
  const versions = window.SIA_DATA.VERSIONS;

  useEffect(() => {
    try {
      const v = JSON.parse(localStorage.getItem('sia_vehicle') || 'null');
      if (v) { setBrand(v.brand || ''); setModel(v.model || ''); setVersion(v.version || ''); }
    } catch {}
  }, []);

  const save = (patch) => {
    const next = { brand, model, version, ...patch };
    localStorage.setItem('sia_vehicle', JSON.stringify(next));
    onChange?.(next);
  };

  const clear = () => {
    setBrand(''); setModel(''); setVersion('');
    localStorage.removeItem('sia_vehicle');
    onChange?.({ brand: '', model: '', version: '' });
  };

  return (
    <div className="vehicle-bar">
      <div className="vehicle-bar-inner">
        <div className="vb-label">
          <Icon name="car" size={14} /> Véhicule
        </div>
        <select className="vb-select" value={brand} onChange={e => { setBrand(e.target.value); setModel(''); setVersion(''); save({ brand: e.target.value, model: '', version: '' }); }}>
          <option value="">Marque</option>
          {BRANDS.map(b => <option key={b} value={b}>{b}</option>)}
        </select>
        <select className="vb-select" value={model} onChange={e => { setModel(e.target.value); save({ model: e.target.value }); }} disabled={!brand}>
          <option value="">Modèle</option>
          {models.map(m => <option key={m} value={m}>{m}</option>)}
        </select>
        <select className="vb-select" value={version} onChange={e => { setVersion(e.target.value); save({ version: e.target.value }); }} disabled={!model}>
          <option value="">Version</option>
          {versions.map(v => <option key={v} value={v}>{v}</option>)}
        </select>
        {brand && (
          <a href={`Search.html?brand=${encodeURIComponent(brand)}${model ? `&model=${encodeURIComponent(model)}` : ''}`} className="btn btn-primary btn-sm" style={{ height: 36 }}>
            <Icon name="search" size={14} /> Rechercher les pièces
          </a>
        )}
        {(brand || model || version) && (
          <button className="vb-clear" onClick={clear}>
            <Icon name="x" size={12} /> Effacer
          </button>
        )}
        <div className="vb-active">
          {brand || model ? (
            <>
              <span>Véhicule actif :</span>
              <strong>{[brand, model, version].filter(Boolean).join(' · ')}</strong>
            </>
          ) : (
            <span>Aucun véhicule sélectionné</span>
          )}
        </div>
      </div>
    </div>
  );
}

function AppFooter() {
  return (
    <footer className="app-footer">
      <div className="app-footer-inner">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <img src="assets/logo.png" alt="SIA" style={{ width: 28, height: 28, objectFit: 'contain' }} />
          <span>© 2026 Sfaxienne Industrielle Automobile — بن جماعة وشركاؤه</span>
          <span style={{ color: 'var(--ink-4)' }}>·</span>
          <span>Plateforme B2B v2.4</span>
        </div>
        <div style={{ display: 'flex', gap: 16 }}>
          <a href="#" style={{ color: 'var(--ink-3)' }}>Conditions</a>
          <a href="#" style={{ color: 'var(--ink-3)' }}>Politique de confidentialité</a>
          <a href="#" style={{ color: 'var(--ink-3)' }}>Support</a>
        </div>
      </div>
    </footer>
  );
}

function SessionTimer() {
  const [time, setTime] = useState(15 * 60);
  useEffect(() => {
    const t = setInterval(() => setTime(s => Math.max(0, s - 1)), 1000);
    return () => clearInterval(t);
  }, []);
  const m = Math.floor(time / 60).toString().padStart(2, '0');
  const s = (time % 60).toString().padStart(2, '0');
  return (
    <div style={{ background: 'oklch(0.97 0.01 60)', borderBottom: '1px solid var(--border)', fontSize: 12 }}>
      <div style={{ maxWidth: 'var(--max)', margin: '0 auto', padding: '8px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, color: 'var(--ink-3)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Icon name="info" size={14} style={{ color: 'var(--warn)' }} />
          <span>Avant de quitter SIA B2B, pensez à <a href="Login.html" style={{ color: 'var(--sia-red)', fontWeight: 600 }}>vous déconnecter</a>.</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontFamily: 'var(--font-mono)', color: 'var(--ink-2)' }}>
          <Icon name="clock" size={12} /> Session : {m}:{s}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { AppHeader, VehicleBar, AppFooter, SessionTimer });
