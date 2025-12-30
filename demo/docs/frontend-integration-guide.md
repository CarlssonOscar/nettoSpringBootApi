# NettoApi - Frontend Integration Guide

> **Purpose**: API integration reference for Vue 3 + TypeScript + PrimeVue  
> **Backend**: Spring Boot 3.x, Java 21  
> **Last Updated**: 2025-12-30

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/regions` | Alla regioner (21 st) |
| `GET` | `/api/v1/municipalities` | Alla kommuner (290 st) |
| `GET` | `/api/v1/municipalities/by-region/{regionId}` | Kommuner i vald region |
| `POST` | `/api/v1/tax/calculate` | Beräkna nettolön |
| `GET` | `/api/v1/tax/calculate?municipalityId=...&grossSalary=...` | Beräkna (alternativ) |

**Base URL**: `http://localhost:8080/api/v1`

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
  municipalityId: string;       // Required
  grossMonthlySalary: number;   // Required, positive
  churchMember?: boolean;       // Default: false
  isPensioner?: boolean;        // Default: false
}

export interface TaxCalculationResponse {
  // Inkomst
  grossMonthlySalary: number;
  grossYearlySalary: number;
  
  // Plats
  municipalityName: string;
  regionName: string;
  
  // Beräkningar (årsvärden)
  taxableIncome: number;
  basicDeduction: number;
  municipalTax: number;
  regionalTax: number;
  stateTax: number;
  burialFee: number;
  churchFee: number;
  
  // Summor
  totalTaxBeforeCredit: number;
  jobTaxCredit: number;
  totalTaxAfterCredit: number;
  yearlyNetSalary: number;
  monthlyNetSalary: number;
  
  // Procentsatser
  effectiveTaxRate: number;
  municipalTaxRate: number;
  regionalTaxRate: number;
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
};
```

```typescript
// src/services/taxService.ts
export const taxService = {
  calculate: (request: TaxCalculationRequest) => 
    api.post<TaxCalculationResponse>('/tax/calculate', request),
};
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
const netSalary = period === 'monthly' ? result.monthlyNetSalary : result.yearlyNetSalary;
const grossSalary = period === 'monthly' ? result.grossMonthlySalary : result.grossYearlySalary;
// Övriga årsfält: result.fieldName / divisor
```

---

## Detaljvy - Beräkningsflöde

Visa i expanderbar sektion ("Visa detaljer"):

| Sektion | Fält | Källa |
|---------|------|-------|
| **Inkomst** | Bruttolön | `grossSalary` |
| | Grundavdrag | `−basicDeduction` |
| | Beskattningsbar inkomst | `taxableIncome` |
| **Skatter** | Kommunalskatt | `municipalTax` (rate: `municipalTaxRate`) |
| | Regionskatt | `regionalTax` (rate: `regionalTaxRate`) |
| | Statlig skatt | `stateTax` (20% över brytpunkt) |
| **Avgifter** | Begravningsavgift | `burialFee` |
| | Kyrkoavgift | `churchFee` (0 om ej medlem) |
| **Subtotal** | Summa före avdrag | `totalTaxBeforeCredit` |
| **Reduktioner** | Jobbskatteavdrag | `−jobTaxCredit` |
| **Resultat** | Slutlig skatt | `totalTaxAfterCredit` |
| | **Nettolön** | `netSalary` |

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

Lägg till i backend för lokal utveckling:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "OPTIONS");
    }
}
```

---

## Environment

```env
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api/v1

# .env.production  
VITE_API_BASE_URL=https://api.example.com/api/v1
```

---

## Error Handling

| Status | Hantering |
|--------|-----------|
| `200` | Visa resultat |
| `400` | Visa `messages[]` array |
| `404` | "Kommun hittades inte" |
| `500` | Generiskt felmeddelande |
| Network | "Kunde inte ansluta till servern" |
