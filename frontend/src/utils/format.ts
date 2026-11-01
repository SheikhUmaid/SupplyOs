const IST = "Asia/Kolkata";

const inr = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });
const count = new Intl.NumberFormat("en-IN");
const date = new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeZone: IST });
const dateTime = new Intl.DateTimeFormat("en-IN", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: IST,
});

export function formatInr(amount: number): string {
  return inr.format(amount);
}

export function formatCount(value: number): string {
  return count.format(value);
}

/** Dates arrive from the API in UTC and always display in IST, whatever the machine's zone. */
export function formatDate(iso: string): string {
  return date.format(new Date(iso));
}

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso));
}
