export const dateLabel = (value: string) => new Intl.DateTimeFormat('uk-UA', { day: 'numeric', month: 'short' }).format(new Date(value + 'T12:00:00'));
export const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };
