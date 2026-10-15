export const formatMinutes = (minutes: number) => minutes < 60 ? `${minutes} хв` : `${Math.floor(minutes / 60)} год${minutes % 60 ? ` ${minutes % 60} хв` : ''}`;
