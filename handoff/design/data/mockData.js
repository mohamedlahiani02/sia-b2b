// SIA B2B — Mock data

const SIA_BRANDS = ['A', 'ABARTH', 'ALFA ROMEO', 'AUDI', 'B', 'BAIC', 'BMW', 'C', 'CHERY', 'CHEVROLET', 'CITROËN', 'D', 'DACIA', 'DAEWOO', 'FIAT', 'FORD', 'HONDA', 'HYUNDAI', 'KIA', 'MAZDA', 'MERCEDES', 'MG', 'MINI', 'MITSUBISHI', 'NISSAN', 'OPEL', 'PEUGEOT', 'PORSCHE', 'RENAULT', 'SEAT', 'SKODA', 'TOYOTA', 'VOLKSWAGEN', 'VOLVO'];

const MODELS_BY_BRAND = {
  AUDI: ['100 C3', '80', 'A3', 'A4 B6', 'A4 B7', 'A6 C5', 'Q5', 'Q7'],
  BMW: ['Série 1 E87', 'Série 3 E46', 'Série 3 E90', 'Série 5 E60', 'X3 E83', 'X5 E70'],
  RENAULT: ['Clio III', 'Clio IV', 'Mégane II', 'Mégane III', 'Kangoo', 'Trafic', 'Master'],
  PEUGEOT: ['206', '207', '208', '307', '308', '407', 'Partner', 'Boxer'],
  VOLKSWAGEN: ['Golf IV', 'Golf V', 'Polo IV', 'Passat B6', 'Tiguan', 'Touareg'],
  NISSAN: ['Micra K12', 'Qashqai', 'X-Trail', 'Urvan E25', 'Navara'],
};

const VERSIONS = ['ESSENCE 1.4 [90CH]', 'ESSENCE 1.6 [110CH]', 'DIESEL 1.9 TDI [105CH]', 'DIESEL 2.0 TDI [140CH]', 'ESSENCE 2.2 [159CH]'];

const SUPPLIERS = ['HELLA', 'BOSCH', 'ELRING', 'TRW', 'SCHAEFFLER', 'CHAMPION', 'BLUE PRINT', 'BILSTEIN', 'VALEO', 'SACHS', 'MAHLE', 'NGK', 'FEBI BILSTEIN', 'KYB', 'BREMBO'];

const FAMILIES = ['Filtration', 'Freinage', 'Moteur', 'Transmission', 'Suspension', 'Électrique', 'Carrosserie', 'Éclairage', 'Chauffage / Clim', 'Direction'];

const PRODUCT_NAMES = [
  'Filtre à huile', 'Filtre à air', 'Filtre à carburant', 'Filtre habitacle',
  'Plaquettes de frein', 'Disque de frein', 'Étrier de frein',
  'Bougie d\'allumage', 'Bobine d\'allumage', 'Alternateur', 'Démarreur',
  'Amortisseur', 'Ressort de suspension', 'Silent-bloc', 'Biellette de barre stabilisatrice',
  'Courroie de distribution', 'Kit de distribution', 'Pompe à eau',
  'Joint de culasse', 'Joint collecteur d\'échappement', 'Joint spi vilebrequin',
  'Rotule de suspension', 'Triangle de suspension', 'Cardan',
  'Klaxon avertisseur sonore', 'Balai d\'essuie-glace', 'Phare avant', 'Feu arrière',
  'Radiateur moteur', 'Ventilateur', 'Thermostat',
  'Guide de soupape', 'Soupape admission', 'Pignon arbre à cames',
];

const STATUSES = [
  { key: 'ok', label: 'En stock', dot: 'ok', badge: 'ok', count: 'ok' },
  { key: 'low', label: 'Stock faible', dot: 'warn', badge: 'warn' },
  { key: 'arrival', label: 'En arrivage', dot: 'info', badge: 'info' },
  { key: 'oos', label: 'Indisponible', dot: 'danger', badge: 'danger' },
];

// Seeded pseudo-random
function seeded(seed) {
  return function() { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; };
}

function generateProducts(count = 38) {
  const rand = seeded(42);
  const pick = (arr) => arr[Math.floor(rand() * arr.length)];
  const products = [];
  for (let i = 0; i < count; i++) {
    const supplier = pick(SUPPLIERS);
    const family = pick(FAMILIES);
    const name = pick(PRODUCT_NAMES);
    const ref = `${String.fromCharCode(65 + Math.floor(rand()*26))}${String.fromCharCode(65 + Math.floor(rand()*26))}${String(Math.floor(rand()*900) + 100)}.${String(Math.floor(rand()*900) + 100)}`;
    const priceBase = Math.round((rand() * 180 + 8) * 1000) / 1000;
    const discount = rand() > 0.4 ? [5, 7, 10, 12, 15][Math.floor(rand()*5)] : 0;
    const price = discount ? Math.round(priceBase * (1 - discount/100) * 1000) / 1000 : priceBase;
    const statusPool = [0, 0, 0, 0, 1, 2, 2, 3];
    const status = STATUSES[statusPool[Math.floor(rand() * statusPool.length)]];
    const stock = status.key === 'ok' ? Math.floor(rand() * 80) + 20
                : status.key === 'low' ? Math.floor(rand() * 5) + 1
                : status.key === 'arrival' ? 0 : 0;
    products.push({
      id: `p${i + 1}`,
      supplier,
      ref,
      siaRef: `SIA${supplier.slice(0,3).toUpperCase()}${String(i).padStart(5, '0')}`,
      name,
      desc: `${name} ${family.toUpperCase()} STD`,
      family,
      oem: [`${Math.floor(rand()*9000000 + 1000000)}`, `${Math.floor(rand()*900)} ${Math.floor(rand()*900)} ${Math.floor(rand()*900)} ${String.fromCharCode(65 + Math.floor(rand()*26))}`],
      oemBrands: [pick(['OE', 'VW', 'AUDI', 'BMW', 'RENAULT', 'NISSAN']), pick(['AUDI', 'BMW', 'FORD', 'SEAT'])],
      priceBase,
      price,
      discount,
      status,
      stock,
      specs: {
        'Type': pick(['Vissé', 'À cartouche', 'En ligne']),
        'Hauteur [mm]': `${Math.floor(rand() * 80) + 40} mm`,
        'Diamètre ext [mm]': `${Math.floor(rand() * 50) + 40} mm`,
        'Filetage': `M${[18, 20, 22, 24][Math.floor(rand()*4)]} x 1,5`,
        'Poids [kg]': `${(rand() * 0.8 + 0.1).toFixed(2)} kg`,
      },
    });
  }
  return products;
}

const PRODUCTS = generateProducts(42);

// Statuts du nouveau processus de commande
// réservée → en attente validation → validée → en préparation → expédiée → livrée → paiement confirmé
// ou: réservée → rejetée

const ORDER_STEPS = [
  { key: 'reserved',   label: 'Bon de commande généré',   icon: 'file' },
  { key: 'pending',    label: 'En attente de validation', icon: 'clock' },
  { key: 'validated',  label: 'Validée par SIA',          icon: 'check' },
  { key: 'preparing',  label: 'En préparation',           icon: 'layers' },
  { key: 'shipped',    label: 'En cours d\'expédition',   icon: 'truck' },
  { key: 'delivered',  label: 'Livrée',                   icon: 'box' },
  { key: 'paid',       label: 'Paiement confirmé',        icon: 'credit' },
];

const ORDERS = [
  { id: 'CV260042346', bc: 'BC26-005421', bl: '—',         stepKey: 'pending',   color: 'warn',   date: '28 avr. 2026', total: 1240.50, items: 8,  client: 'CL0013059', paymentConfirmed: false },
  { id: 'CV260041886', bc: 'BC26-005398', bl: '—',         stepKey: 'reserved',  color: 'info',   date: '27 avr. 2026', total: 342.00,  items: 2,  client: 'CL0013059', paymentConfirmed: false },
  { id: 'CV260041353', bc: 'BC26-005312', bl: 'BL26-04023',stepKey: 'shipped',   color: 'info',   date: '25 avr. 2026', total: 2850.00, items: 14, client: 'CL0013059', paymentConfirmed: false },
  { id: 'CV260041292', bc: 'BC26-005280', bl: 'BL26-04046',stepKey: 'paid',      color: 'ok',     date: '22 avr. 2026', total: 480.25,  items: 3,  client: 'CL0013059', paymentConfirmed: true },
  { id: 'CV260040953', bc: 'BC26-005201', bl: 'BL26-04015',stepKey: 'delivered', color: 'ok',     date: '20 avr. 2026', total: 1670.80, items: 6,  client: 'CL0013059', paymentConfirmed: false },
  { id: 'CV260040952', bc: 'BC26-005199', bl: 'BL26-03995',stepKey: 'paid',      color: 'ok',     date: '18 avr. 2026', total: 920.00,  items: 5,  client: 'CL0013059', paymentConfirmed: true },
  { id: 'CV260040418', bc: 'BC26-005102', bl: 'BL26-03971',stepKey: 'paid',      color: 'ok',     date: '15 avr. 2026', total: 3120.40, items: 12, client: 'CL0013059', paymentConfirmed: true },
  { id: 'CV260040102', bc: 'BC26-005044', bl: '—',         stepKey: 'rejected',  color: 'danger', date: '12 avr. 2026', total: 560.00,  items: 4,  client: 'CL0013059', paymentConfirmed: false, rejectReason: 'Article indisponible — stock épuisé chez fournisseur.' },
];

// Helper: step index from key
function getStepIndex(key) {
  if (key === 'rejected') return -1;
  return ORDER_STEPS.findIndex(s => s.key === key);
}

// Status label + color from stepKey
function getOrderStatus(stepKey) {
  if (stepKey === 'rejected') return { label: 'Rejetée', color: 'danger' };
  const step = ORDER_STEPS.find(s => s.key === stepKey);
  const colorMap = { reserved: 'info', pending: 'warn', validated: 'info', preparing: 'warn', shipped: 'info', delivered: 'ok', paid: 'ok' };
  return { label: step ? step.label : stepKey, color: colorMap[stepKey] || 'info' };
}

// Promotions
const PROMOTIONS = [
  { id: 'promo1', label: 'Remise partenaire SIA', type: 'client', target: 'CL0013059', value: 15, unit: '%', active: true, scope: 'Tous articles' },
  { id: 'promo2', label: 'Promo freinage été 2026', type: 'family', target: 'Freinage', value: 10, unit: '%', active: true, scope: 'Famille Freinage' },
  { id: 'promo3', label: 'Liquidation filtration', type: 'family', target: 'Filtration', value: 20, unit: '%', active: false, scope: 'Famille Filtration' },
  { id: 'promo4', label: 'Client VIP Ben Ayed', type: 'client', target: 'CL0009812', value: 18, unit: '%', active: true, scope: 'Tous articles' },
];

const MESSAGES = [
  { id: 'm1', subject: 'Commande N° B2B/26025481 est envoyé', date: 'Mercredi 18 mars 2026 12:49:03', closed: 'Mercredi 11:49:03', read: false },
  { id: 'm2', subject: 'Commande N° B2B/26024992 est envoyé', date: 'Mardi 17 mars 2026 09:50:18', closed: 'Mardi 08:50:18', read: true },
  { id: 'm3', subject: 'Commande N° B2B/26024783 est envoyé', date: 'Lundi 16 mars 2026 12:22:18', closed: '16 mars 2026 12:22:18', read: true },
  { id: 'm4', subject: 'Commande N° B2B/26023799 est envoyé', date: 'Jeudi 12 mars 2026 13:43:19', closed: '12 mars 2026 13:43:19', read: true },
  { id: 'm5', subject: 'Commande N° B2B/26023174 est envoyé', date: 'Mercredi 11 mars 2026 09:09:09', closed: '11 mars 2026 09:09:09', read: true },
];

const CLIENT = {
  pseudo: 'ETS BEN DJEMAA & CIE',
  code: 'CL0013059',
  status: 'Client Actif',
  name: '—',
  email: 'contact@bendjemaa.tn',
  phone: '+216 74 123 456',
  mobile: '+216 98 765 432',
  address: '220 Av. des Martyrs, Sfax 3000',
  registered: '12 janvier 2022',
  totalOrders: 184,
  totalSpent: 58420.50,
  outstanding: 2340.00,
  paymentMode: { type: 'Effet', days: 60 }, // défini à la création du compte, jamais modifié côté client
};

// Exports
window.SIA_DATA = {
  BRANDS: SIA_BRANDS,
  MODELS_BY_BRAND,
  VERSIONS,
  SUPPLIERS,
  FAMILIES,
  STATUSES,
  PRODUCTS,
  ORDERS,
  ORDER_STEPS,
  getStepIndex,
  getOrderStatus,
  PROMOTIONS,
  MESSAGES,
  CLIENT,
};
