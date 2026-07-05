import { toast } from "sonner";

/**
 * Centralized notification helpers — vaporwave-themed popups
 * for errors, validation, and confirmations across the app.
 */

export const notify = {
  success: (message: string, description?: string) =>
    toast.success(message, { description }),
  error: (message: string, description?: string) =>
    toast.error(message, { description }),
  info: (message: string, description?: string) =>
    toast(message, { description }),
  warning: (message: string, description?: string) =>
    toast.warning(message, { description }),
};

// Common validation popups
export const validation = {
  invalidEmail: () =>
    notify.error("Invalid email", "Please enter a valid email address (e.g. name@domain.com)."),
  requiredField: (field: string) =>
    notify.error("Missing field", `${field} is required.`),
  invalidQuantity: () =>
    notify.error("Invalid quantity", "Quantity must be a positive number greater than 0."),
  quantityExceedsStock: (available: number) =>
    notify.error("Not enough stock", `Only ${available} unit(s) available.`),
  invalidPrice: () =>
    notify.error("Invalid price", "Price must be a positive number."),
  passwordTooShort: (min = 8) =>
    notify.error("Password too short", `Use at least ${min} characters.`),
  networkError: () =>
    notify.error("Network error", "Could not reach the server. Please retry."),
  unauthorized: () =>
    notify.error("Access denied", "You don't have permission to perform this action."),
  saved: (entity = "Changes") => notify.success(`${entity} saved`),
  deleted: (entity = "Item") => notify.success(`${entity} deleted`),
};

// Simple email validator helper
export function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}
