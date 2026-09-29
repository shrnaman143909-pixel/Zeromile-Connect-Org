// Zeromile Connect - TypeScript Core Type Definitions
// Normalized data contracts representing Supabase schema

export type UserRole = 'citizen' | 'municipal_staff' | 'admin';

export interface Profile {
  id: string;
  user_id: string;
  full_name: string | null;
  phone: string | null;
  city: string;
  preferred_language: string;
  created_at: string;
  updated_at: string;
  role?: UserRole;
}

export interface Department {
  id: string;
  name: string;
  description: string | null;
  created_at?: string;
}

export interface Ward {
  id: string;
  ward_number: number;
  name: string;
  city: string;
  created_at?: string;
}

export interface ServiceCategory {
  id: string;
  name: string;
  description: string | null;
  icon: string | null;
  created_at?: string;
}

export interface Service {
  id: string;
  category_id: string;
  department_id: string;
  name: string;
  description: string | null;
  service_type: string;
  is_active: boolean;
  created_at?: string;
  updated_at?: string;
  // Optional joined properties
  category?: ServiceCategory;
  department?: Department;
}

export type NavTab = 'home' | 'services' | 'ai' | 'activity' | 'profile';

export interface ApiResponse<T> {
  data: T | null;
  error: string | null;
  isFallback?: boolean;
}

// Phase 2 Voice Assistant Types
export type VoiceLanguageLocale = 'en-IN' | 'mr-IN' | 'hi-IN';

export interface CivicVoiceRequest {
  inputMode: 'voice' | 'text';
  language: VoiceLanguageLocale;
  transcript: string;
  timestamp: number;
}

