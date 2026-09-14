export function PotIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" className={className} aria-hidden="true">
      <path
        d="M9 3h6M8.5 5.5h7M8 5.5c-2.2 1.7-3.5 4-3.5 6.9C4.5 17.2 7.9 21 12 21s7.5-3.8 7.5-8.6c0-2.9-1.3-5.2-3.5-6.9"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M6 13.5c4 1.6 8 1.6 12 0" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round" />
    </svg>
  );
}
