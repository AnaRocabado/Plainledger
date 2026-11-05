export const PRINCIPLE = "Show the real numbers, not the flattering ones.";

export default function App() {
  return (
    <div className="shell">
      <header className="shell__header">
        <h1 className="shell__brand">Plainledger</h1>
        <p className="shell__principle">{PRINCIPLE}</p>
      </header>
      <main className="shell__main">
        <p>
          A tracker for your crypto purchases and sales: what each purchase really cost, what you actually gained, and
          where you stand today.
        </p>
      </main>
    </div>
  );
}
