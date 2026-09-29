// Zeromile Connect - useSpeechRecognition Hook
// Cross-browser speech recognition support (Web Speech API / SpeechRecognition)
import { useState, useEffect, useRef, useCallback } from 'react';
import { VoiceLanguageLocale } from '../types';

export interface UseSpeechRecognitionReturn {
  isListening: boolean;
  transcript: string;
  interimTranscript: string;
  isSupported: boolean;
  error: string | null;
  startListening: (lang?: VoiceLanguageLocale) => void;
  stopListening: () => void;
  resetTranscript: () => void;
  setManualTranscript: (text: string) => void;
}

export function useSpeechRecognition(defaultLanguage: VoiceLanguageLocale = 'en-IN'): UseSpeechRecognitionReturn {
  const [isListening, setIsListening] = useState<boolean>(false);
  const [transcript, setTranscript] = useState<string>('');
  const [interimTranscript, setInterimTranscript] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [isSupported, setIsSupported] = useState<boolean>(true);

  const recognitionRef = useRef<any>(null);
  const currentLangRef = useRef<VoiceLanguageLocale>(defaultLanguage);

  useEffect(() => {
    currentLangRef.current = defaultLanguage;
  }, [defaultLanguage]);

  useEffect(() => {
    const SpeechRecognition = 
      (typeof window !== 'undefined' && ((window as any).SpeechRecognition || (window as any).webkitSpeechRecognition));

    if (!SpeechRecognition) {
      setIsSupported(false);
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;

      recognition.onstart = () => {
        setIsListening(true);
        setError(null);
      };

      recognition.onresult = (event: any) => {
        let interim = '';
        let final = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          const result = event.results[i];
          const text = result[0].transcript;
          if (result.isFinal) {
            final += text;
          } else {
            interim += text;
          }
        }

        if (interim) {
          setInterimTranscript(interim);
        }

        if (final) {
          setTranscript((prev) => {
            const combined = prev ? `${prev} ${final.trim()}` : final.trim();
            return combined;
          });
          setInterimTranscript('');
        }
      };

      recognition.onerror = (event: any) => {
        setIsListening(false);
        setInterimTranscript('');
        if (event.error === 'not-allowed' || event.error === 'service-not-allowed') {
          setError('Microphone access is needed for voice input.');
        } else if (event.error === 'no-speech') {
          setError("We didn't hear anything. Try speaking again.");
        } else {
          setError('Something went wrong with voice input.');
        }
      };

      recognition.onend = () => {
        setIsListening(false);
        setInterimTranscript('');
      };

      recognitionRef.current = recognition;
    } catch (e: any) {
      setIsSupported(false);
      setError(e?.message || 'Speech recognition initialization failed');
    }

    return () => {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort();
        } catch {
          // ignore cleanup abort
        }
      }
    };
  }, []);

  const startListening = useCallback((lang?: VoiceLanguageLocale) => {
    const selectedLang = lang || currentLangRef.current;
    setError(null);
    setInterimTranscript('');

    if (!recognitionRef.current) {
      setIsSupported(false);
      return;
    }

    try {
      recognitionRef.current.lang = selectedLang;
      recognitionRef.current.start();
    } catch (e: any) {
      // If already started, stop then restart
      try {
        recognitionRef.current.stop();
        setTimeout(() => {
          try {
            recognitionRef.current.lang = selectedLang;
            recognitionRef.current.start();
          } catch {
            // failed restart
          }
        }, 150);
      } catch {
        setError('Something went wrong with voice input.');
      }
    }
  }, []);

  const stopListening = useCallback(() => {
    if (recognitionRef.current && isListening) {
      try {
        recognitionRef.current.stop();
      } catch {
        // ignore
      }
    }
    setIsListening(false);
    setInterimTranscript('');
  }, [isListening]);

  const resetTranscript = useCallback(() => {
    setTranscript('');
    setInterimTranscript('');
    setError(null);
  }, []);

  const setManualTranscript = useCallback((text: string) => {
    setTranscript(text);
  }, []);

  return {
    isListening,
    transcript,
    interimTranscript,
    isSupported,
    error,
    startListening,
    stopListening,
    resetTranscript,
    setManualTranscript,
  };
}
