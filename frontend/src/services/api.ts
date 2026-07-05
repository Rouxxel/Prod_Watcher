export const fakeDelay = (ms = 300) =>
  new Promise<void>((resolve) => setTimeout(resolve, ms));

export const newId = (prefix: string) =>
  `${prefix}_${Math.random().toString(36).slice(2, 9)}`;
