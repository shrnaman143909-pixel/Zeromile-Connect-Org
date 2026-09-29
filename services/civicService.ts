// Zeromile Connect - Civic Data Access Service Layer
// Encapsulates Supabase REST calls (PostgREST API) so UI components remain decoupled.
import { supabaseConfig, isSupabaseConfigured } from '../lib/supabase';
import { Department, Ward, ServiceCategory, Service, Profile, ApiResponse } from '../types';

// Prototype / Demo Seed Data for Nagpur
export const DEMO_DEPARTMENTS: Department[] = [
  { id: 'd1111111-1111-1111-1111-111111111111', name: 'Municipal Corporation', description: 'Nagpur Municipal Corporation central administration and general civic affairs' },
  { id: 'd2222222-2222-2222-2222-222222222222', name: 'Waste Management', description: 'Solid waste collection, street cleaning, sanitation and dumping operations' },
  { id: 'd3333333-3333-3333-3333-333333333333', name: 'Water', description: 'Drinking water supply, pipeline maintenance, tanker allocation and drainage' },
  { id: 'd4444444-4444-4444-4444-444444444444', name: 'Roads', description: 'Road construction, pothole repairs, footpaths, bridges and infrastructure' },
  { id: 'd5555555-5555-5555-5555-555555555555', name: 'Traffic', description: 'Traffic signal management, parking regulations, signages and congestion monitoring' },
  { id: 'd6666666-6666-6666-6666-666666666666', name: 'Pollution', description: 'Air quality, noise control compliance, industrial emissions and environmental protection' },
  { id: 'd7777777-7777-7777-7777-777777777777', name: 'Food Safety', description: 'Hygiene inspections, food vendor licenses and public health safety standards' },
  { id: 'd8888888-8888-8888-8888-888888888888', name: 'Emergency Services', description: 'Fire rescue, disaster management, rapid civic response and fallen trees removal' },
];

export const DEMO_WARDS: Ward[] = [
  { id: 'w1111111-1111-1111-1111-111111111111', ward_number: 32, name: 'Dharampeth', city: 'Nagpur' },
  { id: 'w2222222-2222-2222-2222-222222222222', ward_number: 12, name: 'Sitabuldi', city: 'Nagpur' },
  { id: 'w3333333-3333-3333-3333-333333333333', ward_number: 18, name: 'Ramdaspeth', city: 'Nagpur' },
  { id: 'w4444444-4444-4444-4444-444444444444', ward_number: 25, name: 'Civil Lines', city: 'Nagpur' },
  { id: 'w5555555-5555-5555-5555-555555555555', ward_number: 44, name: 'Sadar', city: 'Nagpur' },
  { id: 'w6666666-6666-6666-6666-666666666666', ward_number: 58, name: 'Manish Nagar', city: 'Nagpur' },
];

export const DEMO_SERVICE_CATEGORIES: ServiceCategory[] = [
  { id: 'c1111111-1111-1111-1111-111111111111', name: 'Road', description: 'Potholes, resurfacing, streetlights, pavements, and dividers', icon: 'road' },
  { id: 'c2222222-2222-2222-2222-222222222222', name: 'Traffic', description: 'Signals, illegal parking, lane marking, and signboards', icon: 'traffic' },
  { id: 'c3333333-3333-3333-3333-333333333333', name: 'Garbage', description: 'Door-to-door collection, street sweeping, and overflowing bins', icon: 'delete' },
  { id: 'c4444444-4444-4444-4444-444444444444', name: 'Water', description: 'Pipeline bursts, water contamination, low pressure, tanker supply', icon: 'water_drop' },
  { id: 'c5555555-5555-5555-5555-555555555555', name: 'Pollution', description: 'Loud noise, factory smoke, dust emissions, open burning', icon: 'air' },
  { id: 'c6666666-6666-6666-6666-666666666666', name: 'Food', description: 'Restaurant hygiene, adulteration, street vendor food safety', icon: 'restaurant' },
  { id: 'c7777777-7777-7777-7777-777777777777', name: 'Certificates', description: 'Birth, death, marriage, trade certificates and municipal licenses', icon: 'description' },
  { id: 'c8888888-8888-8888-8888-888888888888', name: 'Housing', description: 'Property tax queries, unauthorized construction, and encroachment', icon: 'home' },
  { id: 'c9999999-9999-9999-9999-999999999999', name: 'Recycling', description: 'E-waste disposal, plastic collection drives, composting units', icon: 'recycling' },
  { id: 'caaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', name: 'Emergency', description: 'Disaster rescue, fallen trees, flooding waterlogging, fire hazards', icon: 'warning' },
  { id: 'cbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', name: 'Other', description: 'General municipal feedback, community halls, public gardens', icon: 'more_horiz' },
];

export const DEMO_SERVICES: Service[] = [
  {
    id: 's1111111-1111-1111-1111-111111111111',
    category_id: 'c5555555-5555-5555-5555-555555555555',
    department_id: 'd1111111-1111-1111-1111-111111111111',
    name: 'Noise Pollution',
    description: 'Report loud music, illegal loudspeakers, commercial generator noise beyond permissible decibels in residential areas',
    service_type: 'complaint',
    is_active: true,
  },
  {
    id: 's2222222-2222-2222-2222-222222222222',
    category_id: 'c1111111-1111-1111-1111-111111111111',
    department_id: 'd4444444-4444-4444-4444-444444444444',
    name: 'Pothole Complaint',
    description: 'Report dangerous road depressions, potholes, broken tar, or uneven surface risking two-wheeler accidents',
    service_type: 'complaint',
    is_active: true,
  },
  {
    id: 's3333333-3333-3333-3333-333333333333',
    category_id: 'c3333333-3333-3333-3333-333333333333',
    department_id: 'd2222222-2222-2222-2222-222222222222',
    name: 'Garbage Collection',
    description: 'Request waste cleanup for overflowing community bins, skipped daily door-to-door pickup, or illegal street dumps',
    service_type: 'complaint',
    is_active: true,
  },
  {
    id: 's4444444-4444-4444-4444-444444444444',
    category_id: 'c4444444-4444-4444-4444-444444444444',
    department_id: 'd3333333-3333-3333-3333-333333333333',
    name: 'Water Tanker Complaint',
    description: 'Report delays, quality issues, or irregular scheduling for municipal emergency drinking water tanker deliveries',
    service_type: 'complaint',
    is_active: true,
  },
  {
    id: 's5555555-5555-5555-5555-555555555555',
    category_id: 'c7777777-7777-7777-7777-777777777777',
    department_id: 'd1111111-1111-1111-1111-111111111111',
    name: 'Birth Certificate',
    description: 'Apply for new birth registration extract or request corrections in existing civic registry records',
    service_type: 'service',
    is_active: true,
  },
  {
    id: 's6666666-6666-6666-6666-666666666666',
    category_id: 'c6666666-6666-6666-6666-666666666666',
    department_id: 'd7777777-7777-7777-7777-777777777777',
    name: 'Food Safety Complaint',
    description: 'Report unhygienic eateries, food adulteration, stale ingredients, or pest infestation in food businesses',
    service_type: 'complaint',
    is_active: true,
  },
  {
    id: 's7777777-7777-7777-7777-777777777777',
    category_id: 'c1111111-1111-1111-1111-111111111111',
    department_id: 'd4444444-4444-4444-4444-444444444444',
    name: 'Road Maintenance',
    description: 'Request asphalt recarpeting, curb repair, median beautification, and footpath tiling restoration',
    service_type: 'complaint',
    is_active: true,
  },
  {
    id: 's8888888-8888-8888-8888-888888888888',
    category_id: 'c2222222-2222-2222-2222-222222222222',
    department_id: 'd5555555-5555-5555-5555-555555555555',
    name: 'Traffic Complaint',
    description: 'Report non-functioning traffic signal timers, hazardous turning points, or obstructed pedestrian zebra crossings',
    service_type: 'complaint',
    is_active: true,
  },
  {
    id: 's9999999-9999-9999-9999-999999999999',
    category_id: 'c4444444-4444-4444-4444-444444444444',
    department_id: 'd3333333-3333-3333-3333-333333333333',
    name: 'Sewage Overflow',
    description: 'Report blocked municipal drainage lines, backflow in residential areas, or broken manhole covers',
    service_type: 'complaint',
    is_active: true,
  },
  {
    id: 'saaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    category_id: 'caaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    department_id: 'd8888888-8888-8888-8888-888888888888',
    name: 'Emergency Services',
    description: 'Rapid municipal assistance for fallen trees blocking main thoroughfares, flash waterlogging, and safety hazards',
    service_type: 'emergency',
    is_active: true,
  },
];

async function supabaseFetch<T>(endpoint: string): Promise<T> {
  if (!isSupabaseConfigured) {
    throw new Error('Supabase is not configured yet');
  }

  const response = await fetch(`${supabaseConfig.url}/rest/v1/${endpoint}`, {
    headers: {
      apikey: supabaseConfig.key,
      Authorization: `Bearer ${supabaseConfig.key}`,
      'Content-Type': 'application/json',
      Prefer: 'return=representation',
    },
  });

  if (!response.ok) {
    throw new Error(`Supabase query failed: ${response.status} ${response.statusText}`);
  }

  return response.json();
}

export async function getServiceCategories(): Promise<ApiResponse<ServiceCategory[]>> {
  try {
    if (!isSupabaseConfigured) {
      return { data: DEMO_SERVICE_CATEGORIES, error: null, isFallback: true };
    }
    const data = await supabaseFetch<ServiceCategory[]>('service_categories?select=*&order=name.asc');
    return { data, error: null, isFallback: false };
  } catch (err: any) {
    console.error('getServiceCategories error:', err);
    return { data: DEMO_SERVICE_CATEGORIES, error: err.message || 'Failed to fetch categories', isFallback: true };
  }
}

export async function getServices(categoryId?: string): Promise<ApiResponse<Service[]>> {
  try {
    if (!isSupabaseConfigured) {
      const filtered = categoryId ? DEMO_SERVICES.filter(s => s.category_id === categoryId) : DEMO_SERVICES;
      return { data: filtered, error: null, isFallback: true };
    }
    const query = categoryId 
      ? `services?select=*,category:service_categories(*),department:departments(*)&category_id=eq.${categoryId}&is_active=eq.true&order=name.asc`
      : 'services?select=*,category:service_categories(*),department:departments(*)&is_active=eq.true&order=name.asc';
    const data = await supabaseFetch<Service[]>(query);
    return { data, error: null, isFallback: false };
  } catch (err: any) {
    console.error('getServices error:', err);
    const filtered = categoryId ? DEMO_SERVICES.filter(s => s.category_id === categoryId) : DEMO_SERVICES;
    return { data: filtered, error: err.message || 'Failed to fetch services', isFallback: true };
  }
}

export async function getDepartments(): Promise<ApiResponse<Department[]>> {
  try {
    if (!isSupabaseConfigured) {
      return { data: DEMO_DEPARTMENTS, error: null, isFallback: true };
    }
    const data = await supabaseFetch<Department[]>('departments?select=*&order=name.asc');
    return { data, error: null, isFallback: false };
  } catch (err: any) {
    console.error('getDepartments error:', err);
    return { data: DEMO_DEPARTMENTS, error: err.message, isFallback: true };
  }
}

export async function getWards(): Promise<ApiResponse<Ward[]>> {
  try {
    if (!isSupabaseConfigured) {
      return { data: DEMO_WARDS, error: null, isFallback: true };
    }
    const data = await supabaseFetch<Ward[]>('wards?select=*&order=ward_number.asc');
    return { data, error: null, isFallback: false };
  } catch (err: any) {
    console.error('getWards error:', err);
    return { data: DEMO_WARDS, error: err.message, isFallback: true };
  }
}

export async function getProfile(userId: string): Promise<ApiResponse<Profile | null>> {
  try {
    if (!isSupabaseConfigured) {
      return {
        data: {
          id: 'p1111111-1111-1111-1111-111111111111',
          user_id: userId || 'demo-citizen-user',
          full_name: 'Nagpur Citizen',
          phone: '+91 98230 12345',
          city: 'Nagpur',
          preferred_language: 'en',
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
          role: 'citizen',
        },
        error: null,
        isFallback: true,
      };
    }
    const data = await supabaseFetch<Profile[]>(`profiles?user_id=eq.${userId}&select=*`);
    return { data: data[0] || null, error: null, isFallback: false };
  } catch (err: any) {
    console.error('getProfile error:', err);
    return { data: null, error: err.message, isFallback: true };
  }
}
