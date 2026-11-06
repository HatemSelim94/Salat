import type { Location } from './settings.js';

/**
 * Compact city list used for (a) the zero-prompt default location derived from
 * the system time zone and (b) the city picker in settings. Coordinates are
 * city centres rounded to 2 decimals; prayer times are insensitive at that
 * scale (< 1 min). Users can always enter exact coordinates instead.
 */
export interface City {
  name: string;
  country: string;
  lat: number;
  lng: number;
  tz: string;
}

export const CITIES: readonly City[] = [
  // Europe
  { name: 'Berlin', country: 'DE', lat: 52.52, lng: 13.41, tz: 'Europe/Berlin' },
  { name: 'Stuttgart', country: 'DE', lat: 48.78, lng: 9.18, tz: 'Europe/Berlin' },
  { name: 'Munich', country: 'DE', lat: 48.14, lng: 11.58, tz: 'Europe/Berlin' },
  { name: 'Frankfurt', country: 'DE', lat: 50.11, lng: 8.68, tz: 'Europe/Berlin' },
  { name: 'Hamburg', country: 'DE', lat: 53.55, lng: 9.99, tz: 'Europe/Berlin' },
  { name: 'Cologne', country: 'DE', lat: 50.94, lng: 6.96, tz: 'Europe/Berlin' },
  { name: 'Dortmund', country: 'DE', lat: 51.51, lng: 7.47, tz: 'Europe/Berlin' },
  { name: 'Vienna', country: 'AT', lat: 48.21, lng: 16.37, tz: 'Europe/Vienna' },
  { name: 'Zurich', country: 'CH', lat: 47.38, lng: 8.54, tz: 'Europe/Zurich' },
  { name: 'Paris', country: 'FR', lat: 48.86, lng: 2.35, tz: 'Europe/Paris' },
  { name: 'Marseille', country: 'FR', lat: 43.3, lng: 5.37, tz: 'Europe/Paris' },
  { name: 'Brussels', country: 'BE', lat: 50.85, lng: 4.35, tz: 'Europe/Brussels' },
  { name: 'Amsterdam', country: 'NL', lat: 52.37, lng: 4.9, tz: 'Europe/Amsterdam' },
  { name: 'London', country: 'GB', lat: 51.51, lng: -0.13, tz: 'Europe/London' },
  { name: 'Birmingham', country: 'GB', lat: 52.49, lng: -1.89, tz: 'Europe/London' },
  { name: 'Manchester', country: 'GB', lat: 53.48, lng: -2.24, tz: 'Europe/London' },
  { name: 'Dublin', country: 'IE', lat: 53.35, lng: -6.26, tz: 'Europe/Dublin' },
  { name: 'Lisbon', country: 'PT', lat: 38.72, lng: -9.14, tz: 'Europe/Lisbon' },
  { name: 'Madrid', country: 'ES', lat: 40.42, lng: -3.7, tz: 'Europe/Madrid' },
  { name: 'Barcelona', country: 'ES', lat: 41.39, lng: 2.17, tz: 'Europe/Madrid' },
  { name: 'Rome', country: 'IT', lat: 41.9, lng: 12.5, tz: 'Europe/Rome' },
  { name: 'Milan', country: 'IT', lat: 45.46, lng: 9.19, tz: 'Europe/Rome' },
  { name: 'Copenhagen', country: 'DK', lat: 55.68, lng: 12.57, tz: 'Europe/Copenhagen' },
  { name: 'Stockholm', country: 'SE', lat: 59.33, lng: 18.07, tz: 'Europe/Stockholm' },
  { name: 'Malmö', country: 'SE', lat: 55.6, lng: 13.0, tz: 'Europe/Stockholm' },
  { name: 'Oslo', country: 'NO', lat: 59.91, lng: 10.75, tz: 'Europe/Oslo' },
  { name: 'Helsinki', country: 'FI', lat: 60.17, lng: 24.94, tz: 'Europe/Helsinki' },
  { name: 'Warsaw', country: 'PL', lat: 52.23, lng: 21.01, tz: 'Europe/Warsaw' },
  { name: 'Prague', country: 'CZ', lat: 50.08, lng: 14.44, tz: 'Europe/Prague' },
  { name: 'Budapest', country: 'HU', lat: 47.5, lng: 19.04, tz: 'Europe/Budapest' },
  { name: 'Athens', country: 'GR', lat: 37.98, lng: 23.73, tz: 'Europe/Athens' },
  { name: 'Sarajevo', country: 'BA', lat: 43.86, lng: 18.41, tz: 'Europe/Sarajevo' },
  { name: 'Tirana', country: 'AL', lat: 41.33, lng: 19.82, tz: 'Europe/Tirane' },
  { name: 'Skopje', country: 'MK', lat: 41.99, lng: 21.43, tz: 'Europe/Skopje' },
  { name: 'Pristina', country: 'XK', lat: 42.66, lng: 21.17, tz: 'Europe/Belgrade' },
  { name: 'Istanbul', country: 'TR', lat: 41.01, lng: 28.98, tz: 'Europe/Istanbul' },
  { name: 'Ankara', country: 'TR', lat: 39.93, lng: 32.86, tz: 'Europe/Istanbul' },
  { name: 'Moscow', country: 'RU', lat: 55.76, lng: 37.62, tz: 'Europe/Moscow' },
  { name: 'Kazan', country: 'RU', lat: 55.8, lng: 49.11, tz: 'Europe/Moscow' },
  { name: 'Kyiv', country: 'UA', lat: 50.45, lng: 30.52, tz: 'Europe/Kyiv' },
  { name: 'Baku', country: 'AZ', lat: 40.41, lng: 49.87, tz: 'Asia/Baku' },

  // Middle East & North Africa
  { name: 'Cairo', country: 'EG', lat: 30.04, lng: 31.24, tz: 'Africa/Cairo' },
  { name: 'Giza', country: 'EG', lat: 30.01, lng: 31.21, tz: 'Africa/Cairo' },
  { name: 'Alexandria', country: 'EG', lat: 31.2, lng: 29.92, tz: 'Africa/Cairo' },
  { name: 'Makkah', country: 'SA', lat: 21.39, lng: 39.86, tz: 'Asia/Riyadh' },
  { name: 'Madinah', country: 'SA', lat: 24.47, lng: 39.61, tz: 'Asia/Riyadh' },
  { name: 'Riyadh', country: 'SA', lat: 24.71, lng: 46.68, tz: 'Asia/Riyadh' },
  { name: 'Jeddah', country: 'SA', lat: 21.49, lng: 39.19, tz: 'Asia/Riyadh' },
  { name: 'Dubai', country: 'AE', lat: 25.2, lng: 55.27, tz: 'Asia/Dubai' },
  { name: 'Abu Dhabi', country: 'AE', lat: 24.45, lng: 54.38, tz: 'Asia/Dubai' },
  { name: 'Doha', country: 'QA', lat: 25.29, lng: 51.53, tz: 'Asia/Qatar' },
  { name: 'Kuwait City', country: 'KW', lat: 29.38, lng: 47.99, tz: 'Asia/Kuwait' },
  { name: 'Manama', country: 'BH', lat: 26.23, lng: 50.59, tz: 'Asia/Bahrain' },
  { name: 'Muscat', country: 'OM', lat: 23.59, lng: 58.41, tz: 'Asia/Muscat' },
  { name: 'Amman', country: 'JO', lat: 31.95, lng: 35.93, tz: 'Asia/Amman' },
  { name: 'Jerusalem', country: 'PS', lat: 31.78, lng: 35.23, tz: 'Asia/Jerusalem' },
  { name: 'Gaza', country: 'PS', lat: 31.5, lng: 34.47, tz: 'Asia/Gaza' },
  { name: 'Beirut', country: 'LB', lat: 33.89, lng: 35.5, tz: 'Asia/Beirut' },
  { name: 'Damascus', country: 'SY', lat: 33.51, lng: 36.29, tz: 'Asia/Damascus' },
  { name: 'Baghdad', country: 'IQ', lat: 33.31, lng: 44.37, tz: 'Asia/Baghdad' },
  { name: 'Tehran', country: 'IR', lat: 35.69, lng: 51.39, tz: 'Asia/Tehran' },
  { name: 'Sanaa', country: 'YE', lat: 15.37, lng: 44.19, tz: 'Asia/Aden' },
  { name: 'Tripoli', country: 'LY', lat: 32.9, lng: 13.18, tz: 'Africa/Tripoli' },
  { name: 'Tunis', country: 'TN', lat: 36.81, lng: 10.18, tz: 'Africa/Tunis' },
  { name: 'Algiers', country: 'DZ', lat: 36.75, lng: 3.06, tz: 'Africa/Algiers' },
  { name: 'Casablanca', country: 'MA', lat: 33.57, lng: -7.59, tz: 'Africa/Casablanca' },
  { name: 'Rabat', country: 'MA', lat: 34.02, lng: -6.84, tz: 'Africa/Casablanca' },
  { name: 'Khartoum', country: 'SD', lat: 15.5, lng: 32.56, tz: 'Africa/Khartoum' },

  // Sub-Saharan Africa
  { name: 'Lagos', country: 'NG', lat: 6.52, lng: 3.38, tz: 'Africa/Lagos' },
  { name: 'Kano', country: 'NG', lat: 12.0, lng: 8.52, tz: 'Africa/Lagos' },
  { name: 'Abuja', country: 'NG', lat: 9.06, lng: 7.5, tz: 'Africa/Lagos' },
  { name: 'Dakar', country: 'SN', lat: 14.69, lng: -17.44, tz: 'Africa/Dakar' },
  { name: 'Bamako', country: 'ML', lat: 12.64, lng: -8.0, tz: 'Africa/Bamako' },
  { name: 'Accra', country: 'GH', lat: 5.6, lng: -0.19, tz: 'Africa/Accra' },
  { name: 'Addis Ababa', country: 'ET', lat: 9.03, lng: 38.74, tz: 'Africa/Addis_Ababa' },
  { name: 'Mogadishu', country: 'SO', lat: 2.05, lng: 45.32, tz: 'Africa/Mogadishu' },
  { name: 'Nairobi', country: 'KE', lat: -1.29, lng: 36.82, tz: 'Africa/Nairobi' },
  { name: 'Dar es Salaam', country: 'TZ', lat: -6.79, lng: 39.21, tz: 'Africa/Dar_es_Salaam' },
  { name: 'Johannesburg', country: 'ZA', lat: -26.2, lng: 28.04, tz: 'Africa/Johannesburg' },
  { name: 'Cape Town', country: 'ZA', lat: -33.93, lng: 18.42, tz: 'Africa/Johannesburg' },

  // South & Central Asia
  { name: 'Karachi', country: 'PK', lat: 24.86, lng: 67.01, tz: 'Asia/Karachi' },
  { name: 'Lahore', country: 'PK', lat: 31.55, lng: 74.34, tz: 'Asia/Karachi' },
  { name: 'Islamabad', country: 'PK', lat: 33.69, lng: 73.04, tz: 'Asia/Karachi' },
  { name: 'Delhi', country: 'IN', lat: 28.61, lng: 77.21, tz: 'Asia/Kolkata' },
  { name: 'Mumbai', country: 'IN', lat: 19.08, lng: 72.88, tz: 'Asia/Kolkata' },
  { name: 'Hyderabad', country: 'IN', lat: 17.39, lng: 78.49, tz: 'Asia/Kolkata' },
  { name: 'Kolkata', country: 'IN', lat: 22.57, lng: 88.36, tz: 'Asia/Kolkata' },
  { name: 'Dhaka', country: 'BD', lat: 23.81, lng: 90.41, tz: 'Asia/Dhaka' },
  { name: 'Kabul', country: 'AF', lat: 34.53, lng: 69.17, tz: 'Asia/Kabul' },
  { name: 'Tashkent', country: 'UZ', lat: 41.3, lng: 69.24, tz: 'Asia/Tashkent' },
  { name: 'Almaty', country: 'KZ', lat: 43.24, lng: 76.89, tz: 'Asia/Almaty' },
  { name: 'Bishkek', country: 'KG', lat: 42.87, lng: 74.59, tz: 'Asia/Bishkek' },
  { name: 'Dushanbe', country: 'TJ', lat: 38.56, lng: 68.77, tz: 'Asia/Dushanbe' },
  { name: 'Colombo', country: 'LK', lat: 6.93, lng: 79.85, tz: 'Asia/Colombo' },

  // South-East & East Asia
  { name: 'Jakarta', country: 'ID', lat: -6.21, lng: 106.85, tz: 'Asia/Jakarta' },
  { name: 'Surabaya', country: 'ID', lat: -7.26, lng: 112.75, tz: 'Asia/Jakarta' },
  { name: 'Makassar', country: 'ID', lat: -5.15, lng: 119.43, tz: 'Asia/Makassar' },
  { name: 'Kuala Lumpur', country: 'MY', lat: 3.14, lng: 101.69, tz: 'Asia/Kuala_Lumpur' },
  { name: 'Singapore', country: 'SG', lat: 1.35, lng: 103.82, tz: 'Asia/Singapore' },
  { name: 'Bandar Seri Begawan', country: 'BN', lat: 4.9, lng: 114.94, tz: 'Asia/Brunei' },
  { name: 'Manila', country: 'PH', lat: 14.6, lng: 120.98, tz: 'Asia/Manila' },
  { name: 'Bangkok', country: 'TH', lat: 13.76, lng: 100.5, tz: 'Asia/Bangkok' },
  { name: 'Hong Kong', country: 'HK', lat: 22.32, lng: 114.17, tz: 'Asia/Hong_Kong' },
  { name: 'Beijing', country: 'CN', lat: 39.9, lng: 116.4, tz: 'Asia/Shanghai' },
  { name: 'Ürümqi', country: 'CN', lat: 43.83, lng: 87.62, tz: 'Asia/Shanghai' },
  { name: 'Tokyo', country: 'JP', lat: 35.68, lng: 139.69, tz: 'Asia/Tokyo' },
  { name: 'Seoul', country: 'KR', lat: 37.57, lng: 126.98, tz: 'Asia/Seoul' },

  // Americas
  { name: 'New York', country: 'US', lat: 40.71, lng: -74.01, tz: 'America/New_York' },
  { name: 'Washington', country: 'US', lat: 38.91, lng: -77.04, tz: 'America/New_York' },
  { name: 'Toronto', country: 'CA', lat: 43.65, lng: -79.38, tz: 'America/Toronto' },
  { name: 'Montreal', country: 'CA', lat: 45.5, lng: -73.57, tz: 'America/Toronto' },
  { name: 'Chicago', country: 'US', lat: 41.88, lng: -87.63, tz: 'America/Chicago' },
  { name: 'Houston', country: 'US', lat: 29.76, lng: -95.37, tz: 'America/Chicago' },
  { name: 'Dallas', country: 'US', lat: 32.78, lng: -96.8, tz: 'America/Chicago' },
  { name: 'Denver', country: 'US', lat: 39.74, lng: -104.99, tz: 'America/Denver' },
  { name: 'Phoenix', country: 'US', lat: 33.45, lng: -112.07, tz: 'America/Phoenix' },
  { name: 'Los Angeles', country: 'US', lat: 34.05, lng: -118.24, tz: 'America/Los_Angeles' },
  { name: 'San Francisco', country: 'US', lat: 37.77, lng: -122.42, tz: 'America/Los_Angeles' },
  { name: 'Seattle', country: 'US', lat: 47.61, lng: -122.33, tz: 'America/Los_Angeles' },
  { name: 'Vancouver', country: 'CA', lat: 49.28, lng: -123.12, tz: 'America/Vancouver' },
  { name: 'Mexico City', country: 'MX', lat: 19.43, lng: -99.13, tz: 'America/Mexico_City' },
  { name: 'Bogotá', country: 'CO', lat: 4.71, lng: -74.07, tz: 'America/Bogota' },
  { name: 'Lima', country: 'PE', lat: -12.05, lng: -77.04, tz: 'America/Lima' },
  { name: 'São Paulo', country: 'BR', lat: -23.55, lng: -46.63, tz: 'America/Sao_Paulo' },
  { name: 'Buenos Aires', country: 'AR', lat: -34.6, lng: -58.38, tz: 'America/Argentina/Buenos_Aires' },

  // Oceania
  { name: 'Sydney', country: 'AU', lat: -33.87, lng: 151.21, tz: 'Australia/Sydney' },
  { name: 'Melbourne', country: 'AU', lat: -37.81, lng: 144.96, tz: 'Australia/Melbourne' },
  { name: 'Perth', country: 'AU', lat: -31.95, lng: 115.86, tz: 'Australia/Perth' },
  { name: 'Auckland', country: 'NZ', lat: -36.85, lng: 174.76, tz: 'Pacific/Auckland' },
];

/** Case/diacritic-insensitive substring search over name and country. */
export function searchCities(query: string, limit = 8): City[] {
  const q = normalise(query);
  if (!q) return [];
  const starts: City[] = [];
  const contains: City[] = [];
  for (const c of CITIES) {
    const n = normalise(c.name);
    if (n.startsWith(q)) starts.push(c);
    else if (n.includes(q) || c.country.toLowerCase() === q) contains.push(c);
  }
  return [...starts, ...contains].slice(0, limit);
}

function normalise(s: string): string {
  return s
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .trim();
}

export function cityToLocation(c: City): Location {
  return { latitude: c.lat, longitude: c.lng, timezone: c.tz, label: `${c.name}, ${c.country}` };
}

/**
 * Best-effort default location from an IANA time zone: the first bundled city
 * in that zone, else the zone's own city name with no coordinates (caller must
 * then ask the user). Returns null when nothing sensible can be derived.
 */
export function defaultLocationForTimezone(tz: string): Location | null {
  const hit = CITIES.find((c) => c.tz === tz);
  if (hit) return cityToLocation(hit);
  return null;
}

/** The runtime's IANA time zone, or "UTC" if the platform cannot say. */
export function systemTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}
