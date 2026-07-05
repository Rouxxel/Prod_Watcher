import { createFileRoute } from "@tanstack/react-router";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useBusinessMode, type BusinessMode } from "@/hooks/use-business-mode";
import { useWarehouses } from "@/hooks/queries";

export const Route = createFileRoute("/_app/settings")({
  component: SettingsPage,
});

function SettingsPage() {
  const { preference, setPreference, isSingleLocation } = useBusinessMode();
  const warehouses = useWarehouses();
  const count = warehouses.data?.length ?? 0;

  return (
    <div>
      <PageHeader title="Settings" description="Configure company, tax, receipt and integration defaults." />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-base">Business mode</CardTitle>
            <Badge variant="outline" className="text-[10px] uppercase">
              {isSingleLocation ? "Single-location" : "Multi-location"}
            </Badge>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-sm text-muted-foreground">
              ProdWatch adapts to the size of your operation. Small shops can hide warehouse selectors entirely;
              larger companies get full multi-warehouse routing.
            </p>
            <div className="space-y-1.5">
              <Label>Operating mode</Label>
              <Select value={preference} onValueChange={(v) => setPreference(v as BusinessMode)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="auto">Auto-detect ({count} location{count === 1 ? "" : "s"})</SelectItem>
                  <SelectItem value="single">Single-location (small business)</SelectItem>
                  <SelectItem value="multi">Multi-location (warehouses)</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Auto switches based on how many locations you've added.
              </p>
            </div>
          </CardContent>
        </Card>

        <Section title="Company">
          <Field label="Business name" defaultValue="ProdWatch Demo Co." />
          <Field label="Contact email" defaultValue="ops@prodwatch.app" />
        </Section>
        <Section title="Tax">
          <Field label="Default tax rate (%)" defaultValue="16" />
          <Field label="Tax label on receipts" defaultValue="VAT" />
        </Section>
        <Section title="Receipts">
          <Field label="Footer line" defaultValue="Thank you for your purchase!" />
          <Field label="Logo URL" placeholder="https://…" />
        </Section>
        <Section title="Integrations">
          <p className="text-sm text-muted-foreground">No integrations connected. This section is a placeholder for future API providers.</p>
          <Button variant="outline" disabled>Connect provider</Button>
        </Section>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-base">{title}</CardTitle>
        <Badge variant="outline" className="text-[10px] uppercase">Coming soon</Badge>
      </CardHeader>
      <CardContent className="space-y-3">{children}</CardContent>
    </Card>
  );
}

function Field({ label, ...rest }: { label: string } & React.ComponentProps<typeof Input>) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      <Input disabled {...rest} />
    </div>
  );
}
