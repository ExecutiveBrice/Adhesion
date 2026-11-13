import { restoredShopPaymentUrl, shopPaymentReturnUrl } from './shop-payment-return';

describe('Retour HelloAsso avec navigation par fragment', () => {
  ['https://asso.example/', 'https://asso.example/adhesion/'].forEach(baseURI => {
    [false, true].forEach(failed => {
      it(`retrouve la page de paiement (échec : ${failed}, base : ${baseURI})`, () => {
        const callback = shopPaymentReturnUrl(baseURI, 'CMD-2026-000001', failed);
        expect(new URL(callback).hash).toBe('');
        expect(new URL(callback).pathname).toBe(new URL(baseURI).pathname);
        expect(restoredShopPaymentUrl(`${callback}&checkoutIntentId=42`))
          .toBe(`${baseURI}?checkoutIntentId=42#/boutique/commandes/CMD-2026-000001/paiement${failed ? '-echoue' : ''}`);
      });
    });
  });

  it('ignore les URL sans retour boutique ou avec une destination étrangère', () => {
    expect(restoredShopPaymentUrl('https://asso.example/adhesion/#/inscription')).toBeNull();
    for (const route of ['https://evil.example/', '//evil.example/', '/admin', '/boutique/commandes/../paiement']) {
      expect(restoredShopPaymentUrl(`https://asso.example/?shopReturn=${encodeURIComponent(route)}`)).toBeNull();
    }
  });
});
