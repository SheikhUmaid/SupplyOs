export interface Point {
  x: number;
  y: number;
}

export interface Size {
  width: number;
  height: number;
}

/**
 * A simplified outline of Karnataka as [latitude, longitude] pairs. It is schematic, drawn to give
 * warehouses and deliveries a recognisable setting. It is not a survey-accurate boundary.
 */
export const KARNATAKA_OUTLINE: readonly (readonly [number, number])[] = [
  [18.45, 76.9],
  [18.2, 77.6],
  [17.6, 77.7],
  [16.9, 77.5],
  [16.25, 77.55],
  [15.6, 77.3],
  [15.0, 77.2],
  [14.15, 77.3],
  [13.6, 78.0],
  [13.1, 78.5],
  [12.75, 78.2],
  [12.2, 77.7],
  [11.65, 77.5],
  [11.7, 76.6],
  [12.1, 75.9],
  [12.5, 75.2],
  [12.75, 74.85],
  [13.4, 74.65],
  [14.0, 74.4],
  [14.8, 74.1],
  [15.2, 74.2],
  [15.7, 74.3],
  [16.6, 74.5],
  [16.9, 75.2],
  [17.2, 75.6],
  [17.5, 76.2],
  [18.0, 76.4],
];

const LATITUDES = KARNATAKA_OUTLINE.map(([latitude]) => latitude);
const LONGITUDES = KARNATAKA_OUTLINE.map(([, longitude]) => longitude);
const MIN_LAT = Math.min(...LATITUDES);
const MAX_LAT = Math.max(...LATITUDES);
const MIN_LON = Math.min(...LONGITUDES);
const MAX_LON = Math.max(...LONGITUDES);
/** A degree of longitude is shorter than a degree of latitude away from the equator. */
const LON_SCALE = Math.cos((((MIN_LAT + MAX_LAT) / 2) * Math.PI) / 180);

/** Maps a location onto the canvas, keeping the region's proportions and centring it. */
export function project(latitude: number, longitude: number, size: Size, padding = 24): Point {
  const regionWidth = (MAX_LON - MIN_LON) * LON_SCALE;
  const regionHeight = MAX_LAT - MIN_LAT;
  const scale = Math.max(
    0,
    Math.min((size.width - padding * 2) / regionWidth, (size.height - padding * 2) / regionHeight),
  );
  const offsetX = (size.width - regionWidth * scale) / 2;
  const offsetY = (size.height - regionHeight * scale) / 2;
  return {
    x: offsetX + (longitude - MIN_LON) * LON_SCALE * scale,
    y: offsetY + (MAX_LAT - latitude) * scale,
  };
}

export function outlinePoints(size: Size): number[] {
  return KARNATAKA_OUTLINE.flatMap(([latitude, longitude]) => {
    const { x, y } = project(latitude, longitude, size);
    return [x, y];
  });
}
