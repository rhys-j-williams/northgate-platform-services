/**
 * Word lists the generator draws from. Every name here is invented. Merchant names are plausible
 * but fictional, because screenshots of this data are shown outside the bank.
 */

export const GIVEN_NAMES = [
  'Marisol', 'Devraj', 'Anneke', 'Terrence', 'Ilyana', 'Cormac', 'Nadira', 'Emeka', 'Solveig',
  'Rafael', 'Yolanda', 'Kwame', 'Beatriz', 'Anders', 'Priyanka', 'Lucian', 'Odile', 'Bartholomew',
  'Sanaa', 'Theodora', 'Hiroshi', 'Camille', 'Idris', 'Rosalind', 'Casimir', 'Delphine', 'Amadou',
  'Grethe', 'Vikram', 'Josephine', 'Tomás', 'Ngozi', 'Halvard', 'Constance', 'Ravi', 'Marguerite'
];

export const FAMILY_NAMES = [
  'Ashcombe', 'Beauchêne', 'Castellar', 'Dunmore', 'Ekstrand', 'Fairweather', 'Grimaldi',
  'Halloran', 'Ivarsson', 'Jandali', 'Kettering', 'Lindqvist', 'Moreau', 'Nkemdirim', 'Ollivander',
  'Pemberton', 'Quintanilla', 'Ravenhill', 'Stellenbosch', 'Thackeray', 'Ubadike', 'Vasquez',
  'Winterbourne', 'Xiong', 'Yarborough', 'Zabala', 'Fitzhugh', 'Marchetti', 'Okonjo', 'Prendergast'
];

export const ORGANISATION_NAMES = [
  'Bramblewood Joinery', 'Cardinal Point Logistics', 'Delta Harbour Seafoods', 'Eastvale Dental',
  'Foxglove Landscaping', 'Granary Lane Bakehouse', 'Hollow Creek Cabinetry', 'Ironleaf Roofing',
  'Juniper Bend Vineyards', 'Kestrel Freight', 'Larkspur Physiotherapy', 'Millrace Print Works',
  'Northgate Veterinary', 'Orchard Row Grocers', 'Pinnacle Ridge Surveying', 'Quarry Street Tyres',
  'Redwing Electrical', 'Saltmarsh Catering', 'Thistledown Textiles', 'Umberfield Machining'
];

export const TREASURY_ORGANISATIONS = [
  'Ardent Chemical Holdings', 'Bellwether Aggregates', 'Coastline Energy Partners',
  'Drumlin Pharmaceutical', 'Evermark Industrial Group', 'Fenwick Maritime',
  'Glasshouse Media Group', 'Harrowgate Automotive'
];

export const CITIES: Array<[string, string, string]> = [
  ['Charlotte', 'NC', '28202'], ['Concord', 'NC', '28025'], ['Plano', 'TX', '75024'],
  ['Richardson', 'TX', '75080'], ['Jersey City', 'NJ', '07302'], ['Hoboken', 'NJ', '07030'],
  ['Chandler', 'AZ', '85225'], ['Boise', 'ID', '83702'], ['Dayton', 'OH', '45402'],
  ['Savannah', 'GA', '31401'], ['Providence', 'RI', '02903'], ['Spokane', 'WA', '99201'],
  ['Fort Collins', 'CO', '80521'], ['Chattanooga', 'TN', '37402'], ['Bangor', 'ME', '04401']
];

export const STREETS = [
  'Alder Ridge Road', 'Beckett Lane', 'Cranmere Street', 'Dunlin Way', 'Elderbrook Drive',
  'Foundry Row', 'Gainsborough Avenue', 'Hartsfield Close', 'Ironwood Terrace', 'Jessamine Street',
  'Kingfisher Walk', 'Longmeadow Boulevard', 'Marlstone Court', 'Nettlebed Road', 'Overton Crescent'
];

export interface MerchantSeed {
  name: string;
  mcc: string;
  category: string;
  low: number;
  high: number;
}

/** Amount ranges are in whole dollars and are used to keep statements believable. */
export const MERCHANTS: MerchantSeed[] = [
  { name: 'Harvest Row Market', mcc: '5411', category: 'groceries', low: 18, high: 214 },
  { name: 'Copperline Grocers', mcc: '5411', category: 'groceries', low: 22, high: 186 },
  { name: 'Thornbury Provisions', mcc: '5499', category: 'groceries', low: 7, high: 64 },
  { name: 'The Salted Fig', mcc: '5812', category: 'dining', low: 14, high: 132 },
  { name: 'Basalt Coffee House', mcc: '5814', category: 'dining', low: 3, high: 21 },
  { name: 'Kettle & Vine', mcc: '5812', category: 'dining', low: 26, high: 178 },
  { name: 'Northgate Fuel', mcc: '5541', category: 'fuel', low: 24, high: 96 },
  { name: 'Wayfarer Service Station', mcc: '5541', category: 'fuel', low: 28, high: 104 },
  { name: 'Cirroline Airways', mcc: '3000', category: 'travel', low: 128, high: 1240 },
  { name: 'Meridian Trust Bank Travel Centre', mcc: '4722', category: 'travel', low: 96, high: 2400 },
  { name: 'Lodgewood Inns', mcc: '7011', category: 'travel', low: 88, high: 640 },
  { name: 'Cascade Power & Light', mcc: '4900', category: 'utilities', low: 46, high: 288 },
  { name: 'Orbit Fibre Communications', mcc: '4814', category: 'utilities', low: 39, high: 149 },
  { name: 'Municipal Water Authority', mcc: '4900', category: 'utilities', low: 28, high: 132 },
  { name: 'Willowbank Family Practice', mcc: '8011', category: 'healthcare', low: 25, high: 480 },
  { name: 'Ashfield Pharmacy', mcc: '5912', category: 'healthcare', low: 8, high: 178 },
  { name: 'Rill & Reed Cinema', mcc: '7832', category: 'entertainment', low: 11, high: 68 },
  { name: 'Stavros Streaming', mcc: '5815', category: 'entertainment', low: 7, high: 24 },
  { name: 'Halcyon Fitness Club', mcc: '7997', category: 'entertainment', low: 32, high: 96 },
  { name: 'Beaufort Mutual Insurance', mcc: '6300', category: 'insurance', low: 84, high: 412 },
  { name: 'Stonefield Hardware', mcc: '5200', category: 'home-improvement', low: 12, high: 640 },
  { name: 'Verity College Bursar', mcc: '8220', category: 'education', low: 240, high: 3200 },
  { name: 'Riverbend Animal Shelter', mcc: '8398', category: 'charity', low: 10, high: 250 },
  { name: 'County Revenue Office', mcc: '9311', category: 'taxes', low: 120, high: 2800 }
];

export const PAYEE_NAMES = [
  'Cascade Power & Light', 'Orbit Fibre Communications', 'Beaufort Mutual Insurance',
  'Municipal Water Authority', 'Verity College Bursar', 'Halcyon Fitness Club',
  'Meridian Trust Bank Auto Loan', 'Larkspur Physiotherapy', 'County Revenue Office'
];

export const ALERT_CATALOGUE: Array<{
  code: string; label: string; description: string; regulatory: boolean;
}> = [
  {
    code: 'BALANCE_LOW', label: 'Low balance',
    description: 'Sent when an account balance falls below the amount you choose.',
    regulatory: false
  },
  {
    code: 'LARGE_TRANSACTION', label: 'Large transaction',
    description: 'Sent when a transaction over your chosen amount posts to an account.',
    regulatory: false
  },
  {
    code: 'CARD_DECLINED', label: 'Card declined',
    description: 'Sent when a card transaction is declined.',
    regulatory: false
  },
  {
    code: 'DEPOSIT_POSTED', label: 'Deposit posted',
    description: 'Sent when a deposit or incoming transfer posts.',
    regulatory: false
  },
  {
    code: 'PAYLINK_RECEIVED', label: 'PayLink money received',
    description: 'Sent when someone sends you money through PayLink.',
    regulatory: false
  },
  {
    code: 'OVERDRAFT_NOTICE', label: 'Overdraft notice',
    description: 'Required notice sent when an account is overdrawn. This alert cannot be turned off.',
    regulatory: true
  },
  {
    code: 'REG_E_ERROR_RESOLUTION', label: 'Dispute status',
    description: 'Required updates on the progress of a transaction dispute. This alert cannot be turned off.',
    regulatory: true
  },
  {
    code: 'PRIVACY_NOTICE', label: 'Annual privacy notice',
    description: 'Required annual notice describing how your information is used. This alert cannot be turned off.',
    regulatory: true
  },
  {
    code: 'RATE_CHANGE', label: 'Rate or fee change',
    description: 'Required notice of a change to the rates or fees on an account. This alert cannot be turned off.',
    regulatory: true
  },
  {
    code: 'SECURITY_SIGN_IN', label: 'New device sign in',
    description: 'Sent when your profile is accessed from a device Keystone has not seen before.',
    regulatory: false
  }
];
