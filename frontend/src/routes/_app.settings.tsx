import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useBusinessMode, type BusinessMode } from "@/hooks/use-business-mode";
import { useWarehouses } from "@/hooks/queries";
import { useSettings, useUpdateSettings } from "@/hooks/use-settings";
import { toastApiError } from "@/lib/api-error";
import { dateTime } from "@/lib/format";
import { isValidEmail, notify, validation } from "@/lib/notify";
import type { WorkspaceSettings, WorkspaceSettingsUpdate } from "@/types";

export const Route = createFileRoute("/_app/settings")({
  component: SettingsPage,
});

interface SettingsForm {
  businessName: string;
  contactEmail: string;
  taxRatePercent: string;
  taxLabel: string;
  receiptFooter: string;
  receiptLogoUrl: string;
}

function settingsToForm(settings: WorkspaceSettings): SettingsForm {
  return {
    businessName: settings.businessName,
    contactEmail: settings.contactEmail,
    taxRatePercent: String(Math.round(settings.taxRate * 100)),
    taxLabel: settings.taxLabel,
    receiptFooter: settings.receiptFooter ?? "",
    receiptLogoUrl: settings.receiptLogoUrl ?? "",
  };
}

function buildPatch(form: SettingsForm, settings: WorkspaceSettings): WorkspaceSettingsUpdate | null {
  const patch: WorkspaceSettingsUpdate = {};
  const name = form.businessName.trim();
  const email = form.contactEmail.trim();
  const footer = form.receiptFooter.trim() || null;
  const logoUrl = form.receiptLogoUrl.trim() || null;
  const taxRate = Number(form.taxRatePercent) / 100;

  if (name !== settings.businessName) patch.businessName = name;
  if (email !== settings.contactEmail) patch.contactEmail = email;
  if (Math.abs(taxRate - settings.taxRate) > 0.0001) patch.taxRate = taxRate;
  if (form.taxLabel.trim() !== settings.taxLabel) patch.taxLabel = form.taxLabel.trim();
  if (footer !== settings.receiptFooter) patch.receiptFooter = footer;
  if (logoUrl !== settings.receiptLogoUrl) patch.receiptLogoUrl = logoUrl;

  return Object.keys(patch).length > 0 ? patch : null;
}

function isFormDirty(form: SettingsForm, settings: WorkspaceSettings): boolean {
  return buildPatch(form, settings) !== null;
}

function SettingsPage() {
  const { data: settings, isLoading } = useSettings();
  const updateSettings = useUpdateSettings();
  const { preference, setPreference, isSingleLocation } = useBusinessMode();
  const warehouses = useWarehouses();
  const count = warehouses.data?.length ?? 0;

  const [form, setForm] = useState<SettingsForm | null>(null);
  const [logoPreviewError, setLogoPreviewError] = useState(false);

  useEffect(() => {
    if (!settings) return;
    setForm(settingsToForm(settings));
    setLogoPreviewError(false);
  }, [settings]);

  const dirty = useMemo(
    () => (form && settings ? isFormDirty(form, settings) : false),
    [form, settings],
  );

  const handleBusinessModeChange = (mode: BusinessMode) => {
    const previous = preference;
    setPreference(mode);
    updateSettings.mutate(
      { businessMode: mode },
      {
        onSuccess: () => notify.success("Business mode updated"),
        onError: (err) => {
          setPreference(previous);
          toastApiError(err, "Could not update business mode");
        },
      },
    );
  };

  const handleSave = () => {
    if (!form || !settings) return;

    const name = form.businessName.trim();
    if (!name) {
      validation.requiredField("Business name");
      return;
    }

    const email = form.contactEmail.trim();
    if (!email || !isValidEmail(email)) {
      validation.invalidEmail();
      return;
    }

    const taxPercent = Number(form.taxRatePercent);
    if (!Number.isFinite(taxPercent) || taxPercent < 0 || taxPercent > 100) {
      notify.error("Invalid tax rate", "Enter a percentage between 0 and 100.");
      return;
    }

    const logoUrl = form.receiptLogoUrl.trim();
    if (logoUrl && !/^https?:\/\/.+/i.test(logoUrl)) {
      notify.error("Invalid logo URL", "Use an http or https URL.");
      return;
    }

    const patch = buildPatch(form, settings);
    if (!patch) return;

    updateSettings.mutate(patch, {
      onSuccess: () => validation.saved("Settings"),
      onError: (err) => toastApiError(err, "Could not save settings"),
    });
  };

  if (isLoading || !form || !settings) {
    return (
      <div>
        <PageHeader title="Settings" description="Configure company, tax, receipt and integration defaults." />
        <div className="grid gap-4 lg:grid-cols-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-48" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Settings"
        description={
          settings.updatedAt
            ? `Configure company, tax, receipt and integration defaults. Last updated ${dateTime(settings.updatedAt)}.`
            : "Configure company, tax, receipt and integration defaults."
        }
      />

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
              <Select
                value={preference}
                onValueChange={(v) => handleBusinessModeChange(v as BusinessMode)}
                disabled={updateSettings.isPending}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="auto">
                    Auto-detect ({count} location{count === 1 ? "" : "s"})
                  </SelectItem>
                  <SelectItem value="single">Single-location (small business)</SelectItem>
                  <SelectItem value="multi">Multi-location (warehouses)</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Auto switches based on how many locations you've added. Saved immediately.
              </p>
            </div>
          </CardContent>
        </Card>

        <SettingsCard title="Company">
          <div className="space-y-1.5">
            <Label htmlFor="business-name">Business name</Label>
            <Input
              id="business-name"
              value={form.businessName}
              onChange={(e) => setForm((f) => (f ? { ...f, businessName: e.target.value } : f))}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="contact-email">Contact email</Label>
            <Input
              id="contact-email"
              type="email"
              value={form.contactEmail}
              onChange={(e) => setForm((f) => (f ? { ...f, contactEmail: e.target.value } : f))}
              required
            />
          </div>
        </SettingsCard>

        <SettingsCard title="Tax">
          <div className="space-y-1.5">
            <Label htmlFor="tax-rate">Default tax rate (%)</Label>
            <Input
              id="tax-rate"
              type="number"
              min={0}
              max={100}
              step={0.01}
              value={form.taxRatePercent}
              onChange={(e) => setForm((f) => (f ? { ...f, taxRatePercent: e.target.value } : f))}
            />
            <p className="text-xs text-muted-foreground">
              Applied to new checkouts; server validates totals.
            </p>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="tax-label">Tax label on receipts</Label>
            <Input
              id="tax-label"
              value={form.taxLabel}
              onChange={(e) => setForm((f) => (f ? { ...f, taxLabel: e.target.value } : f))}
            />
          </div>
        </SettingsCard>

        <SettingsCard title="Receipts">
          <div className="space-y-1.5">
            <Label htmlFor="receipt-footer">Footer line</Label>
            <Textarea
              id="receipt-footer"
              value={form.receiptFooter}
              onChange={(e) => setForm((f) => (f ? { ...f, receiptFooter: e.target.value } : f))}
              maxLength={500}
              rows={2}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="receipt-logo">Logo URL</Label>
            <Input
              id="receipt-logo"
              type="url"
              placeholder="https://…"
              value={form.receiptLogoUrl}
              onChange={(e) => {
                setLogoPreviewError(false);
                setForm((f) => (f ? { ...f, receiptLogoUrl: e.target.value } : f));
              }}
            />
            <p className="text-xs text-muted-foreground">HTTPS preferred. Max 2048 characters.</p>
            {form.receiptLogoUrl.trim() && !logoPreviewError ? (
              <img
                src={form.receiptLogoUrl.trim()}
                alt="Receipt logo preview"
                className="mt-2 h-12 max-w-full rounded border border-border object-contain"
                onError={() => setLogoPreviewError(true)}
              />
            ) : form.receiptLogoUrl.trim() && logoPreviewError ? (
              <p className="text-xs text-muted-foreground">Preview unavailable for this URL.</p>
            ) : null}
          </div>
        </SettingsCard>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-base">Integrations</CardTitle>
            <Badge variant="outline" className="text-[10px] uppercase">
              Planned
            </Badge>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-sm text-muted-foreground">
              No integrations connected. This section is a placeholder for future API providers.
            </p>
            <Button variant="outline" disabled>
              Connect provider
            </Button>
          </CardContent>
        </Card>
      </div>

      <div className="sticky bottom-4 mt-6 flex justify-end">
        <Button
          onClick={handleSave}
          disabled={!dirty || updateSettings.isPending}
        >
          {updateSettings.isPending ? "Saving…" : "Save changes"}
        </Button>
      </div>
    </div>
  );
}

function SettingsCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">{children}</CardContent>
    </Card>
  );
}
