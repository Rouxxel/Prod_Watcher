import { useEffect, useRef, useState } from "react";
import { Plus, Upload, X } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { Product, ProductInput, Warehouse } from "@/types";
import { ProductImageCarousel } from "./ProductImageCarousel";
import { useBusinessMode } from "@/hooks/use-business-mode";
import {
  buildProductImagePath,
  isProductImageUploadEnabled,
  uploadProductImage,
} from "@/lib/storage";
import { notify } from "@/lib/notify";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initial?: Product | null;
  warehouses: Warehouse[];
  onSubmit: (input: ProductInput) => void;
  pending?: boolean;
}

const empty: ProductInput = {
  name: "",
  sku: "",
  category: "",
  price: 0,
  stock: 0,
  warehouseId: "",
  lowStockThreshold: 5,
  images: [],
};

export function ProductFormDialog({ open, onOpenChange, initial, warehouses, onSubmit, pending }: Props) {
  const [form, setForm] = useState<ProductInput>(empty);
  const [imageUrl, setImageUrl] = useState("");
  const [uploadingImage, setUploadingImage] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const uploadEnabled = isProductImageUploadEnabled();
  const { isSingleLocation } = useBusinessMode();

  useEffect(() => {
    if (open) {
      setImageUrl("");
      setForm(
        initial
          ? {
              name: initial.name,
              sku: initial.sku,
              category: initial.category,
              price: initial.price,
              stock: initial.stock,
              warehouseId: initial.warehouseId,
              lowStockThreshold: initial.lowStockThreshold,
              images: [...initial.images],
            }
          : { ...empty, warehouseId: warehouses[0]?.id ?? "" },
      );
    }
  }, [open, initial, warehouses]);

  const set = <K extends keyof ProductInput>(key: K, value: ProductInput[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  const addImage = () => {
    const url = imageUrl.trim();
    if (!url) return;
    set("images", [...form.images, url]);
    setImageUrl("");
  };

  const removeImage = (idx: number) =>
    set(
      "images",
      form.images.filter((_, i) => i !== idx),
    );

  const handleFileUpload = async (file: File) => {
    setUploadingImage(true);
    try {
      const path = buildProductImagePath(file, form.sku || undefined);
      const url = await uploadProductImage(file, path);
      set("images", [...form.images, url]);
      notify.success("Image uploaded");
    } catch (err) {
      notify.error(
        "Upload failed",
        err instanceof Error ? err.message : "Could not upload image.",
      );
    } finally {
      setUploadingImage(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{initial ? "Edit product" : "Add product"}</DialogTitle>
        </DialogHeader>
        <form
          className="space-y-3"
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit(form);
          }}
        >
          <div className="space-y-2">
            <Label>Photos</Label>
            <ProductImageCarousel
              images={form.images}
              alt={form.name || "Product"}
              aspect="aspect-[4/3]"
              className="w-full"
            />
            <div className="flex flex-wrap gap-2">
              <Input
                placeholder="Paste an image URL…"
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addImage();
                  }
                }}
                className="min-w-[200px] flex-1"
              />
              <Button type="button" variant="secondary" onClick={addImage}>
                <Plus className="mr-1 h-4 w-4" /> Add URL
              </Button>
              {uploadEnabled && (
                <>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) void handleFileUpload(file);
                    }}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    disabled={uploadingImage || pending}
                    onClick={() => fileInputRef.current?.click()}
                  >
                    <Upload className="mr-1 h-4 w-4" />
                    {uploadingImage ? "Uploading…" : "Upload file"}
                  </Button>
                </>
              )}
            </div>
            {!uploadEnabled && (
              <p className="text-xs text-muted-foreground">
                Set <code className="text-foreground/80">VITE_SUPABASE_URL</code> and{" "}
                <code className="text-foreground/80">VITE_SUPABASE_ANON_KEY</code> to enable file uploads.
                URL paste works without them.
              </p>
            )}
            {form.images.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {form.images.map((src, idx) => (
                  <div key={idx} className="relative h-12 w-12 overflow-hidden rounded-md border border-border">
                    <img src={src} alt="" className="h-full w-full object-cover" />
                    <button
                      type="button"
                      onClick={() => removeImage(idx)}
                      aria-label="Remove image"
                      className="absolute right-0 top-0 grid h-4 w-4 place-items-center rounded-bl-md bg-background/80 text-foreground hover:bg-destructive hover:text-destructive-foreground"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="name">Name</Label>
            <Input id="name" value={form.name} onChange={(e) => set("name", e.target.value)} required />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="sku">SKU</Label>
              <Input id="sku" value={form.sku} onChange={(e) => set("sku", e.target.value)} required />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="category">Category</Label>
              <Input id="category" value={form.category} onChange={(e) => set("category", e.target.value)} required />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="price">Price</Label>
              <Input
                id="price"
                type="number"
                step="0.01"
                value={form.price}
                onChange={(e) => set("price", parseFloat(e.target.value) || 0)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="stock">Stock</Label>
              <Input
                id="stock"
                type="number"
                value={form.stock}
                onChange={(e) => set("stock", parseInt(e.target.value, 10) || 0)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="low">Low at</Label>
              <Input
                id="low"
                type="number"
                value={form.lowStockThreshold}
                onChange={(e) => set("lowStockThreshold", parseInt(e.target.value, 10) || 0)}
              />
            </div>
          </div>
          {!isSingleLocation && (
            <div className="space-y-1.5">
              <Label>Warehouse</Label>
              <Select value={form.warehouseId} onValueChange={(v) => set("warehouseId", v)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {warehouses.map((w) => (
                    <SelectItem key={w.id} value={w.id}>
                      {w.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
          <DialogFooter className="pt-2">
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending || uploadingImage}>
              {initial ? "Save changes" : "Create product"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
