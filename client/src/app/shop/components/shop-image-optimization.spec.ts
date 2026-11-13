import { optimizeShopImage, SHOP_IMAGE_TARGET_BYTES } from './shop-image-optimization';

describe('Optimisation des images de la boutique', () => {
  const oversized = () => new Blob([new Uint8Array(SHOP_IMAGE_TARGET_BYTES + 1)], { type: 'image/jpeg' });
  const compressed = () => new Blob(['JPEG compressé'], { type: 'image/jpeg' });

  function canvasWith(encode: (quality: number) => Blob | null): HTMLCanvasElement {
    return {
      width: 800, height: 600,
      toBlob: jasmine.createSpy('toBlob').and.callFake((callback: BlobCallback, type: string, quality: number) => callback(encode(quality)))
    } as unknown as HTMLCanvasElement;
  }

  it('conserve la qualité élevée lorsque le poids est déjà adapté', async () => {
    const blob = compressed();
    const canvas = canvasWith(() => blob);
    expect(await optimizeShopImage(canvas)).toBe(blob);
    expect(canvas.toBlob).toHaveBeenCalledOnceWith(jasmine.any(Function), 'image/jpeg', 0.9);
  });

  it('réduit progressivement la qualité jusqu’à respecter le budget de poids', async () => {
    const qualities: number[] = [];
    const small = compressed();
    const large = oversized();
    const canvas = canvasWith(quality => {
      qualities.push(quality);
      return quality <= 0.7 ? small : large;
    });
    const result = await optimizeShopImage(canvas);
    expect(qualities).toEqual([0.9, 0.8, 0.7]);
    expect(result).toBe(small);
    expect(result.size).toBeLessThanOrEqual(SHOP_IMAGE_TARGET_BYTES);
  });

  it('réduit aussi la résolution si la compression seule ne suffit pas', async () => {
    const original = canvasWith(oversized);
    const small = compressed();
    const resized = canvasWith(() => small);
    const drawImage = jasmine.createSpy('drawImage');
    resized.getContext = jasmine.createSpy('getContext').and.returnValue({ drawImage });
    spyOn(document, 'createElement').and.returnValue(resized);
    expect(await optimizeShopImage(original)).toBe(small);
    expect(resized.width).toBe(640);
    expect(resized.height).toBe(480);
    expect(drawImage).toHaveBeenCalledWith(original, 0, 0, 640, 480);
    expect(resized.toBlob).toHaveBeenCalledOnceWith(jasmine.any(Function), 'image/jpeg', 0.9);
  });

  it('signale un échec d’encodage au lieu de renvoyer un fichier vide', async () => {
    await expectAsync(optimizeShopImage(canvasWith(() => null))).toBeRejected();
  });

  it('arrête les tentatives si l’encodeur ne parvient jamais à réduire le fichier', async () => {
    const original = canvasWith(oversized);
    let attempts = 0;
    spyOn(document, 'createElement').and.callFake(() => {
      attempts++;
      const resized = canvasWith(oversized);
      resized.getContext = jasmine.createSpy('getContext').and.returnValue({ drawImage: () => undefined });
      return resized;
    });
    await expectAsync(optimizeShopImage(original)).toBeRejected();
    expect(attempts).toBeLessThan(20);
  });
});
