// Budget adapté aux visuels du catalogue, avant tout envoi au serveur.
export const SHOP_IMAGE_TARGET_BYTES = 250 * 1024;

export async function optimizeShopImage(canvas: HTMLCanvasElement): Promise<Blob> {
  let output = canvas;
  while (true) {
    for (const quality of [0.9, 0.8, 0.7, 0.6, 0.5]) {
      const blob = await new Promise<Blob | null>(resolve => output.toBlob(resolve, 'image/jpeg', quality));
      if (!blob) throw new Error('Compression de l’image indisponible');
      if (blob.size <= SHOP_IMAGE_TARGET_BYTES) return blob;
    }

    // Si la compression seule ne suffit pas, réduire aussi la résolution.
    if (output.width <= 64) throw new Error('Compression de l’image impossible');
    const smaller = document.createElement('canvas');
    smaller.width = Math.max(64, Math.floor(output.width * 0.8 / 4) * 4);
    smaller.height = Math.round(smaller.width * canvas.height / canvas.width);
    const context = smaller.getContext('2d');
    if (!context) throw new Error('Canvas indisponible');
    context.drawImage(canvas, 0, 0, smaller.width, smaller.height);
    output = smaller;
  }
}
