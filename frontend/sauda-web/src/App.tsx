export default function App() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <main className="mx-auto flex max-w-3xl flex-col gap-8 px-6 py-24">
        <div className="flex items-center gap-4">
          <span className="text-2xl font-semibold tracking-tight text-emerald-300">Sauda</span>
          <span className="rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-300">
            MVP Foundation
          </span>
        </div>
        <h1 className="text-4xl font-semibold tracking-tight">Sauda B2B Platform</h1>
        <p className="text-lg text-slate-300">
          Compare supplier prices, analyze differences, build carts, and submit orders — built for
          small retail stores.
        </p>
        <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-6 text-sm text-slate-300">
          <p className="font-medium text-slate-100">Engineering foundation ready</p>
          <ul className="mt-3 list-disc space-y-1 pl-5">
            <li>Spring Boot API with profile-based integrations</li>
            <li>React frontend with TypeScript and Tailwind CSS</li>
            <li>Docker Compose stack with Nginx, PostgreSQL, CI/CD pipelines</li>
          </ul>
        </div>
      </main>
    </div>
  );
}
