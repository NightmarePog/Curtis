import Link from "next/link";
import { Compass } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <div className="mx-auto flex min-h-[55vh] max-w-md flex-col items-center justify-center gap-5 text-center">
      <span className="flex size-14 items-center justify-center rounded-2xl bg-brand-soft text-brand">
        <Compass aria-hidden="true" className="size-6" />
      </span>
      <div className="space-y-2">
        <p
          data-numeric
          className="text-sm font-medium tracking-wide text-muted-foreground"
        >
          404
        </p>
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">
          Tady nic není
        </h1>
        <p className="text-pretty text-sm text-muted-foreground">
          Stránka neexistuje nebo byla přesunuta.
        </p>
      </div>
      <Button asChild>
        <Link href="/dashboard">Zpět na přehled</Link>
      </Button>
    </div>
  );
}
