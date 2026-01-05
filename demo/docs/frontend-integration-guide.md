# NettoApi - Frontend Integration Guide

> **Purpose**: API integration reference for Vue 3 + TypeScript + PrimeVue  
> **Backend**: Spring Boot 3.x, Java 21  
> **Tax Year**: 2026 (SKV 433)  
> **Last Updated**: 2026-01-05

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/regions` | Alla regioner (21 st) |
| `GET` | `/api/v1/municipalities` | Alla kommuner (290 st) |
| `GET` | `/api/v1/municipalities/by-region/{regionId}` | Kommuner i vald region |
| `POST` | `/api/v1/tax/calculate` | Beräkna nettolön (med UUID) |
| `GET` | `/api/v1/tax/calculate-by-code` | Beräkna nettolön (med kommunkod) |

**Base URL**: `http://localhost:8080/api/v1`

> ⚠️ **OBS**: All kommunikation sker via API Gateway på port 8080. Backend-tjänsten körs internt på port 8181 men ska aldrig anropas direkt från frontend.

**OpenAPI**: `GET /api/v1/api-docs`

---

## TypeScript Types

```typescript
// src/types/index.ts

export interface Region {
  id: string;        // UUID
  code: string;      // "24"
  name: string;      // "Västerbotten"
}

export interface Municipality {
  id: string;        // UUID
  code: string;      // "2480"
  name: string;      // "Umeå"
  regionId: string;
}

export interface TaxCalculationRequest {
  municipalityId: string;       // Required (UUID)
  grossMonthlySalary: number;   // Required, positive
  churchMember?: boolean;       // Default: false
  isPensioner?: boolean;        // Default: false
}

// Alternativt för calculate-by-code endpoint
export interface TaxCalculationByCodeParams {
  municipalityCode: string;     // Required, e.g. "2480"
  grossSalary: number;          // Required, positive
  churchMember?: boolean;       // Default: false
  isPensioner?: boolean;        // Default: false
}

export interface TaxCalculationResponse {
  // Identifiering
  municipalityId: string;
  municipalityName: string;
  regionName: string;
  
  // Inkomst
  grossMonthlySalary: number;
  grossYearlySalary: number;
  
  // Skattesatser (decimal, t.ex. 0.228 = 22.8%)
  municipalTaxRate: number;
  regionalTaxRate: number;
  stateTaxRate: number;
  burialFeeRate: number;
  churchFeeRate: number;
  
  // Avdrag (årsvärden)
  yearlyBasicDeduction: number;
  yearlyJobTaxCredit: number;
  
  // Beräknade skatter (årsvärden)
  yearlyTaxableIncome: number;
  yearlyMunicipalTax: number;
  yearlyRegionalTax: number;
  yearlyStateTax: number;
  yearlyBurialFee: number;
  yearlyChurchFee: number;
  yearlyTotalTax: number;
  
  // Månadsvärden
  monthlyTotalTax: number;
  netMonthlySalary: number;
  
  // Summering
  effectiveTaxRate: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message?: string;
  messages?: string[];  // Valideringsfel
}
```

---

## API Services

```typescript
// src/services/api.ts
import axios from 'axios';

// API Gateway URL - all requests go through port 8080
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
});
```

```typescript
// src/services/municipalityService.ts
export const municipalityService = {
  getRegions: () => api.get<Region[]>('/regions'),
  getMunicipalitiesByRegion: (regionId: string) => 
    api.get<Municipality[]>(`/municipalities/by-region/${regionId}`),
  getAllMunicipalities: () => api.get<Municipality[]>('/municipalities'),
};
```

```typescript
// src/services/taxService.ts
export const taxService = {
  // Beräkna med UUID (POST)
  calculate: (request: TaxCalculationRequest) => 
    api.post<TaxCalculationResponse>('/tax/calculate', request),
  
  // Beräkna med kommunkod (GET) - enklare för snabblänkar
  calculateByCode: (params: TaxCalculationByCodeParams) => 
    api.get<TaxCalculationResponse>('/tax/calculate-by-code', { params }),
};
```

### Exempel: calculate-by-code

```typescript
// Enkel beräkning med kommunkod
const result = await taxService.calculateByCode({
  municipalityCode: '2480',  // Umeå
  grossSalary: 37500,
  churchMember: false,
  isPensioner: false
});

console.log(result.data.netMonthlySalary);  // 29250.12
console.log(result.data.monthlyTotalTax);   // 8249.88
```

---

## Composables

### useMunicipalities

Hanterar region → kommun-filtrering.

```typescript
// src/composables/useMunicipalities.ts
export function useMunicipalities() {
  const regions = ref<Region[]>([]);
  const municipalities = ref<Municipality[]>([]);
  const selectedRegionId = ref<string | null>(null);
  const selectedMunicipalityId = ref<string | null>(null);
  const loading = ref(false);

  // Ladda regioner vid mount
  onMounted(async () => {
    const { data } = await municipalityService.getRegions();
    regions.value = data.sort((a, b) => a.name.localeCompare(b.name, 'sv'));
  });

  // När region ändras → ladda kommuner
  watch(selectedRegionId, async (regionId) => {
    if (!regionId) {
      municipalities.value = [];
      selectedMunicipalityId.value = null;
      return;
    }
    loading.value = true;
    const { data } = await municipalityService.getMunicipalitiesByRegion(regionId);
    municipalities.value = data.sort((a, b) => a.name.localeCompare(b.name, 'sv'));
    selectedMunicipalityId.value = null;
    loading.value = false;
  });

  return { regions, municipalities, selectedRegionId, selectedMunicipalityId, loading };
}
```

### useTaxCalculator

Hanterar beräkning och resultat.

```typescript
// src/composables/useTaxCalculator.ts
export function useTaxCalculator() {
  const result = ref<TaxCalculationResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function calculate(request: TaxCalculationRequest) {
    loading.value = true;
    error.value = null;
    try {
      const { data } = await taxService.calculate(request);
      result.value = data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Kunde inte beräkna';
    } finally {
      loading.value = false;
    }
  }

  return { result, loading, error, calculate };
}
```

---

## Component Structure

```
┌─────────────────────────────────────────────────────────┐
│                    TaxCalculatorPage                     │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────┐    │
│  │              TaxInputForm                        │    │
│  │  • Dropdown: Region                             │    │
│  │  • Dropdown: Kommun (filtreras på region)       │    │
│  │  • InputNumber: Bruttolön                       │    │
│  │  • Checkbox: Kyrkomedlem, Pensionär             │    │
│  │  • Button: Beräkna                              │    │
│  └─────────────────────────────────────────────────┘    │
│                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐    │
│  │   TaxResultCard      │  │   TaxResultCard      │    │
│  │   period="monthly"   │  │   period="yearly"    │    │
│  └──────────────────────┘  └──────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### TaxInputForm

| Prop | Type | Description |
|------|------|-------------|
| `loading` | `boolean` | Disable button under beräkning |

| Emit | Payload | Description |
|------|---------|-------------|
| `submit` | `TaxCalculationRequest` | Formulärdata vid submit |

### TaxResultCard

| Prop | Type | Description |
|------|------|-------------|
| `result` | `TaxCalculationResponse` | Beräkningsresultat |
| `period` | `'monthly' \| 'yearly'` | Visar månads- eller årsvärden |

**Beräkningslogik för period:**
```typescript
const divisor = period === 'monthly' ? 12 : 1;

// Direkt från API
const netSalary = period === 'monthly' 
  ? result.netMonthlySalary 
  : result.grossYearlySalary - result.yearlyTotalTax;

const grossSalary = period === 'monthly' 
  ? result.grossMonthlySalary 
  : result.grossYearlySalary;

const totalTax = period === 'monthly'
  ? result.monthlyTotalTax
  : result.yearlyTotalTax;

// Övriga årsfält divideras för månadsvisning
const municipalTax = result.yearlyMunicipalTax / divisor;
const regionalTax = result.yearlyRegionalTax / divisor;
const stateTax = result.yearlyStateTax / divisor;
const burialFee = result.yearlyBurialFee / divisor;
const churchFee = result.yearlyChurchFee / divisor;
const basicDeduction = result.yearlyBasicDeduction / divisor;
const jobTaxCredit = result.yearlyJobTaxCredit / divisor;
```

---

## Detaljvy - Beräkningsflöde

Visa i expanderbar sektion ("Visa detaljer"):

| Sektion | Fält | API-fält | Formel |
|---------|------|----------|--------|
| **Inkomst** | Bruttolön | `grossYearlySalary` | |
| | Grundavdrag | `yearlyBasicDeduction` | SKV 433 §6.1 |
| | Beskattningsbar inkomst | `yearlyTaxableIncome` | brutto - grundavdrag |
| **Skatter** | Kommunalskatt | `yearlyMunicipalTax` | BFI × `municipalTaxRate` |
| | Regionskatt | `yearlyRegionalTax` | BFI × `regionalTaxRate` |
| | Statlig skatt | `yearlyStateTax` | 20% över 643 000 kr |
| **Avgifter** | Begravningsavgift | `yearlyBurialFee` | BFI × `burialFeeRate` |
| | Kyrkoavgift | `yearlyChurchFee` | 0 om ej medlem |
| **Reduktioner** | Jobbskatteavdrag | `yearlyJobTaxCredit` | SKV 433 §7.5.2 |
| **Resultat** | Total skatt | `yearlyTotalTax` | |
| | Månadsskatt | `monthlyTotalTax` | total / 12 |
| | **Nettolön** | `netMonthlySalary` | brutto - månadsskatt |
| | Effektiv skattesats | `effectiveTaxRate` | total / brutto |

> **SKV 433**: Beräkningarna följer Skatteverkets tekniska beskrivning för skattetabeller 2026.
> Inkluderar allmän pensionsavgift (7%), skattereduktioner och public service-avgift.

---

## PrimeVue Components

Rekommenderade komponenter:

| Fält | PrimeVue Component |
|------|-------------------|
| Region/Kommun | `Dropdown` med `filter` |
| Bruttolön | `InputNumber` med `currency="SEK"` |
| Checkboxar | `Checkbox` |
| Beräkna-knapp | `Button` |
| Resultat-kort | `Card` |
| Detaljer | `Accordion` eller `Panel` |
| Tabell | `DataTable` (readonly) |

---

## CORS (Backend)

CORS är konfigurerat i API Gateway på port 8080. Frontend-applikationer på följande origins tillåts:

- `http://localhost:5173` (Vite dev server)
- `http://localhost:3000` (alternativ dev port)

> **Notera**: Anropa alltid API:et via port 8080 (API Gateway), aldrig direkt mot backend på port 8181.

---

## Environment

```env
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api/v1

# .env.production  
VITE_API_BASE_URL=https://api.example.com/api/v1
```

> ⚠️ **Viktigt**: Använd alltid API Gateway URL, inte direkt backend-URL.

---

## Error Handling

| Status | Hantering |
|--------|-----------|
| `200` | Visa resultat |
| `400` | Visa `messages[]` array (valideringsfel) |
| `404` | "Kommun hittades inte" |
| `500` | Generiskt felmeddelande |
| Network | "Kunde inte ansluta till servern" |

---

## Exempel: Komplett API-svar

```json
{
  "municipalityId": "bc208ea4-81dc-4ddb-be51-321e2ffc0f35",
  "municipalityName": "UMEÅ",
  "regionName": "Västerbottens län",
  "grossMonthlySalary": 37500,
  "grossYearlySalary": 450000,
  "municipalTaxRate": 0.228,
  "regionalTaxRate": 0.1185,
  "stateTaxRate": 0.2,
  "burialFeeRate": 0.00292,
  "churchFeeRate": 0,
  "yearlyBasicDeduction": 19000,
  "yearlyJobTaxCredit": 51285,
  "yearlyTaxableIncome": 431000,
  "yearlyMunicipalTax": 98268,
  "yearlyRegionalTax": 51073.5,
  "yearlyStateTax": 0,
  "yearlyBurialFee": 1258,
  "yearlyChurchFee": 0,
  "yearlyTotalTax": 98998.5,
  "monthlyTotalTax": 8249.88,
  "netMonthlySalary": 29250.12,
  "effectiveTaxRate": 0.22
}
```

---

## Formatering för visning

```typescript
// src/utils/formatters.ts

export const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('sv-SE', {
    style: 'currency',
    currency: 'SEK',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value);
};

export const formatPercent = (value: number): string => {
  return new Intl.NumberFormat('sv-SE', {
    style: 'percent',
    minimumFractionDigits: 1,
    maximumFractionDigits: 2
  }).format(value);
};

// Exempel:
// formatCurrency(29250.12)  → "29 250 kr"
// formatPercent(0.228)      → "22,8 %"
```
