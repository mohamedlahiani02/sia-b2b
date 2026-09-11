// SIA B2B — Product components

const { useState: useS, useEffect: useE } = React;

// Placeholder product image — colored silhouette based on family
function ProductImage({ product, size = 64 }) {
  const family = product?.family || '';
  const iconName = {
    'Filtration': 'layers',
    'Freinage': 'alert',
    'Moteur': 'settings',
    'Transmission': 'refresh',
    'Suspension': 'chart',
    'Électrique': 'sparkle',
    'Carrosserie': 'box',
    'Éclairage': 'sparkle',
    'Chauffage / Clim': 'refresh',
    'Direction': 'arrowRight',
  }[family] || 'box';
  return (
    <div className="product-img" style={{ width: size, height: size }}>
      <Icon name={iconName} size={Math.floor(size * 0.4)} />
    </div>
  );
}

function StatusPill({ status }) {
  return (
    <span className={`badge badge-${status.badge}`}>
      <span className={`dot dot-${status.dot}`}></span>
      {status.label}
    </span>
  );
}

function PriceCell({ product }) {
  const intPart = Math.floor(product.price);
  const decPart = Math.round((product.price - intPart) * 1000).toString().padStart(3, '0');
  return (
    <div>
      <div className="mono tnum" style={{ fontSize: 18, fontWeight: 600, color: 'var(--ink)', letterSpacing: '-0.01em' }}>
        {intPart}<span style={{ fontSize: 12, color: 'var(--ink-3)' }}>,{decPart}</span>
        <span style={{ fontSize: 11, color: 'var(--ink-3)', marginLeft: 4, fontWeight: 500 }}>DT</span>
      </div>
      {product.discount > 0 && (
        <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 2 }}>
          <span style={{ textDecoration: 'line-through' }}>Au lieu de {product.priceBase.toFixed(3)} DT</span>
          <span style={{ color: 'var(--sia-red)', fontWeight: 600, marginLeft: 6 }}>−{product.discount}%</span>
        </div>
      )}
    </div>
  );
}

function QtyStepper({ value = 1, onChange, max = 99 }) {
  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', border: '1px solid var(--border)', borderRadius: 6, overflow: 'hidden', background: 'var(--surface)' }}>
      <button
        onClick={() => onChange?.(Math.max(1, value - 1))}
        style={{ width: 30, height: 32, display: 'grid', placeItems: 'center', color: 'var(--ink-3)' }}
      ><Icon name="minus" size={12} /></button>
      <input
        type="number"
        value={value}
        onChange={e => onChange?.(Math.max(1, Math.min(max, parseInt(e.target.value) || 1)))}
        style={{ width: 44, height: 32, textAlign: 'center', border: 'none', background: 'transparent', fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 600, outline: 'none' }}
      />
      <button
        onClick={() => onChange?.(Math.min(max, value + 1))}
        style={{ width: 30, height: 32, display: 'grid', placeItems: 'center', color: 'var(--ink-3)' }}
      ><Icon name="plus" size={12} /></button>
    </div>
  );
}

// Cart utilities (localStorage)
const Cart = {
  get() {
    try { return JSON.parse(localStorage.getItem('sia_cart') || '[]'); } catch { return []; }
  },
  set(items) {
    localStorage.setItem('sia_cart', JSON.stringify(items));
    window.dispatchEvent(new CustomEvent('cart-changed'));
  },
  add(product, qty = 1) {
    const items = this.get();
    const existing = items.find(i => i.id === product.id);
    if (existing) existing.qty += qty;
    else items.push({ id: product.id, qty, supplier: product.supplier, ref: product.ref, name: product.name, price: product.price, priceBase: product.priceBase, discount: product.discount });
    this.set(items);
  },
  remove(id) { this.set(this.get().filter(i => i.id !== id)); },
  setQty(id, qty) {
    const items = this.get();
    const it = items.find(i => i.id === id);
    if (it) { it.qty = Math.max(1, qty); this.set(items); }
  },
  count() { return this.get().reduce((s, i) => s + i.qty, 0); },
  total() { return this.get().reduce((s, i) => s + i.qty * i.price, 0); },
};

function useCart() {
  const [items, setItems] = useS(Cart.get());
  useE(() => {
    const h = () => setItems(Cart.get());
    window.addEventListener('cart-changed', h);
    window.addEventListener('storage', h);
    return () => { window.removeEventListener('cart-changed', h); window.removeEventListener('storage', h); };
  }, []);
  return { items, count: items.reduce((s, i) => s + i.qty, 0), total: items.reduce((s, i) => s + i.qty * i.price, 0) };
}

// Product row (table)
function ProductRow({ product, onInfo, onCompatibles }) {
  const [qty, setQty] = useS(1);
  const [added, setAdded] = useS(false);
  const handleAdd = () => {
    Cart.add(product, qty);
    setAdded(true);
    setTimeout(() => setAdded(false), 1500);
  };
  const disabled = product.status.key === 'oos' || product.status.key === 'arrival';
  return (
    <tr>
      <td style={{ width: 80 }}>
        <ProductImage product={product} size={56} />
      </td>
      <td style={{ minWidth: 180 }}>
        <div style={{ fontWeight: 700, fontSize: 13, letterSpacing: '-0.01em' }}>{product.supplier}</div>
        <div className="mono" style={{ fontSize: 12, color: 'var(--ink-2)', marginTop: 2 }}>{product.ref}</div>
        <div className="mono" style={{ fontSize: 10, color: 'var(--ink-4)', marginTop: 4 }}>Réf. SIA : {product.siaRef}</div>
      </td>
      <td style={{ minWidth: 220 }}>
        <div style={{ fontWeight: 600, textTransform: 'uppercase', fontSize: 13, letterSpacing: '-0.005em' }}>{product.name}</div>
        <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 3 }}>({product.desc})</div>
      </td>
      <td style={{ minWidth: 150 }}>
        {(product.status.key === 'ok' || product.status.key === 'low') ? <PriceCell product={product} /> : (
          <StatusPill status={product.status} />
        )}
      </td>
      <td>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {(product.status.key === 'ok' || product.status.key === 'low') && (
            <>
              <QtyStepper value={qty} onChange={setQty} />
              <button
                className="btn btn-primary btn-sm"
                onClick={handleAdd}
                disabled={disabled}
                style={{ padding: '8px 10px' }}
                title="Ajouter au panier"
              >
                {added ? <Icon name="check" size={14} /> : <Icon name="cart" size={14} />}
              </button>
            </>
          )}
          {product.status.key === 'ok' && <span className="dot dot-ok" title="En stock"></span>}
          {product.status.key === 'low' && <span className="dot dot-warn" title="Stock faible"></span>}
        </div>
      </td>
      <td style={{ minWidth: 140, fontSize: 11 }}>
        <div style={{ color: 'var(--ink-4)', marginBottom: 2 }}>OE</div>
        {product.oem.map((o, i) => (
          <div key={i} style={{ display: 'flex', gap: 8, alignItems: 'baseline' }}>
            <span style={{ fontSize: 10, color: 'var(--ink-3)', width: 40 }}>{product.oemBrands[i] || 'OE'}</span>
            <span className="mono" style={{ fontSize: 11, color: 'var(--ink-2)' }}>{o}</span>
          </div>
        ))}
      </td>
      <td style={{ width: 140 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <button className="btn btn-dark btn-sm" onClick={() => onInfo?.(product)} style={{ justifyContent: 'center' }}>
            <Icon name="info" size={12} /> Info d'article
          </button>
          <button className="btn btn-ghost btn-sm" onClick={() => onCompatibles?.(product)} style={{ justifyContent: 'center' }}>
            <Icon name="layers" size={12} /> Compatibles
          </button>
        </div>
      </td>
    </tr>
  );
}

// Product detail modal
function ProductModal({ product, onClose, onShowCompatibles }) {
  if (!product) return null;
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ width: 900 }}>
        <div className="modal-head" style={{ padding: 0, borderBottom: 'none', flexDirection: 'column', alignItems: 'stretch' }}>
          <div style={{ padding: '20px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{ width: 44, height: 44, background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 8, display: 'grid', placeItems: 'center', fontWeight: 800, fontSize: 11, letterSpacing: '0.04em' }}>{product.supplier.slice(0,3)}</div>
              <div>
                <div style={{ fontSize: 11, color: 'var(--ink-3)', fontWeight: 600, letterSpacing: '0.05em', textTransform: 'uppercase' }}>Fiche article</div>
                <div style={{ fontSize: 16, fontWeight: 700, letterSpacing: '-0.01em' }}>{product.name}</div>
              </div>
            </div>
            <button className="icon-btn" onClick={onClose}><Icon name="x" size={18} /></button>
          </div>
          <div style={{ background: 'var(--ink)', color: 'white', padding: '14px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ fontSize: 15, fontWeight: 600, letterSpacing: '0.02em' }}>
              <span style={{ textTransform: 'uppercase' }}>{product.name}</span>
              <span style={{ margin: '0 10px', color: 'oklch(0.6 0.01 250)' }}>·</span>
              <span className="mono">{product.supplier} {product.ref}</span>
            </div>
            <div style={{ fontSize: 12, color: 'oklch(0.75 0.01 250)' }}>
              {product.family}
            </div>
          </div>
        </div>
        <div className="modal-body">
          <div style={{ display: 'grid', gridTemplateColumns: '260px 1fr', gap: 32 }}>
            <div>
              <ProductImage product={product} size={240} />
              <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
                <Icon name="info" size={12} /> Image illustrative
              </div>
              {(product.status.key === 'ok' || product.status.key === 'low') && (
                <div style={{ marginTop: 20, padding: 16, background: 'var(--surface-2)', borderRadius: 10, border: '1px solid var(--border)' }}>
                  <div className="label">Prix unitaire HT</div>
                  <PriceCell product={product} />
                </div>
              )}
            </div>
            <div>
              <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', rowGap: 10, fontSize: 13 }}>
                <div style={{ color: 'var(--ink-3)' }}>Référence SIA</div>
                <div className="mono" style={{ fontWeight: 600 }}>{product.siaRef}</div>
                <div style={{ color: 'var(--ink-3)' }}>Désignation TecDoc</div>
                <div>{product.name}</div>
                <div style={{ color: 'var(--ink-3)' }}>Désignation technique</div>
                <div style={{ textTransform: 'uppercase' }}>{product.name.toUpperCase()}</div>
                <div style={{ color: 'var(--ink-3)' }}>Famille</div>
                <div>{product.family}</div>
                <div style={{ color: 'var(--ink-3)' }}>Constructeur</div>
                <div className="muted">—</div>
              </div>
              <div style={{ marginTop: 24, borderTop: '1px solid var(--border)', paddingTop: 20 }}>
                <div className="label" style={{ marginBottom: 12 }}>Caractéristiques techniques</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0 }}>
                  {Object.entries(product.specs).map(([k, v], i) => (
                    <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 14px', background: i % 2 ? 'var(--surface-2)' : 'transparent', borderRadius: 6 }}>
                      <span style={{ color: 'var(--ink-3)', fontSize: 12 }}>{k}</span>
                      <span className="mono" style={{ fontWeight: 600, fontSize: 12 }}>{v}</span>
                    </div>
                  ))}
                </div>
              </div>
              <div style={{ marginTop: 24, borderTop: '1px solid var(--border)', paddingTop: 20 }}>
                <div className="label" style={{ marginBottom: 12 }}>Références d'origine (OEM)</div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {product.oem.map((o, i) => (
                    <div key={i} style={{ padding: '6px 10px', background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 6, fontSize: 12 }}>
                      <span style={{ color: 'var(--ink-3)', marginRight: 8 }}>{product.oemBrands[i] || 'OE'}</span>
                      <span className="mono" style={{ fontWeight: 600 }}>{o}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
        <div className="modal-foot">
          <button className="btn btn-ghost" onClick={() => onShowCompatibles?.(product)}>
            <Icon name="layers" size={14} /> Voir les pièces compatibles
          </button>
          <div style={{ flex: 1 }} />
          {(product.status.key === 'ok' || product.status.key === 'low') && (
            <button className="btn btn-primary" onClick={() => { Cart.add(product); onClose?.(); }}>
              <Icon name="cart" size={14} /> Ajouter au panier
            </button>
          )}
          <button className="btn btn-ghost" onClick={onClose}>Fermer</button>
        </div>
      </div>
    </div>
  );
}

// Compatibles modal
function CompatiblesModal({ product, onClose }) {
  if (!product) return null;
  const compatibles = window.SIA_DATA.PRODUCTS
    .filter(p => p.family === product.family && p.id !== product.id)
    .slice(0, 6);
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ width: 1000 }}>
        <div className="modal-head">
          <div>
            <div style={{ fontSize: 11, color: 'var(--ink-3)', fontWeight: 600, letterSpacing: '0.05em', textTransform: 'uppercase', marginBottom: 4 }}>Équivalence de la pièce</div>
            <div style={{ fontSize: 16, fontWeight: 700 }}>Pièces compatibles avec <span style={{ color: 'var(--sia-red)' }}>{product.supplier} {product.ref}</span></div>
          </div>
          <button className="icon-btn" onClick={onClose}><Icon name="x" size={18} /></button>
        </div>
        <div className="modal-body">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 16 }}>
            {compatibles.map(p => (
              <div key={p.id} className="card" style={{ padding: 14 }}>
                <div style={{ position: 'relative' }}>
                  <ProductImage product={p} size={180} />
                  <div style={{ position: 'absolute', top: 8, left: 8, padding: '2px 8px', background: 'white', border: '1px solid var(--border)', borderRadius: 999, fontSize: 10, fontWeight: 700, letterSpacing: '0.04em' }}>{p.supplier}</div>
                </div>
                <div style={{ fontSize: 10, color: 'var(--ink-3)', marginTop: 10, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <Icon name="info" size={10} /> Photo générique
                </div>
                <div style={{ marginTop: 8, fontSize: 13, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '-0.005em' }}>{p.name}</div>
                <div className="mono" style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 4 }}>{p.supplier} · {p.ref}</div>
                <div style={{ marginTop: 10, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  {(p.status.key === 'ok' || p.status.key === 'low') ? (
                    <div className="mono tnum" style={{ fontWeight: 700, fontSize: 14 }}>{p.price.toFixed(3)} <span style={{ fontSize: 10, color: 'var(--ink-3)' }}>DT</span></div>
                  ) : <StatusPill status={p.status} />}
                  {(p.status.key === 'ok' || p.status.key === 'low') && (
                    <button className="btn btn-dark btn-sm" onClick={() => { Cart.add(p); }} style={{ padding: '6px 8px' }}>
                      <Icon name="cart" size={12} />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="modal-foot">
          <button className="btn btn-ghost" onClick={onClose}>Fermer</button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { ProductImage, StatusPill, PriceCell, QtyStepper, Cart, useCart, ProductRow, ProductModal, CompatiblesModal });
