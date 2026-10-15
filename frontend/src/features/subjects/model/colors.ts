export const colors = ['#81583d', '#a3734e', '#b38b59', '#91634f', '#73754f', '#776153'];
// Match subjects created with the original palette to the current theme.
const legacyColors: Record<string, string> = { '#6262d9': colors[0], '#208579': colors[4], '#c27b35': colors[2], '#cc617c': colors[3], '#438bc1': colors[1], '#8870ad': colors[5] };
export const subjectColor = (color: string) => legacyColors[color.toLowerCase()] || color;
