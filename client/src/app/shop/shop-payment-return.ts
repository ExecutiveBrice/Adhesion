const PAYMENT_RETURN_PARAM = 'shopReturn';
const PAYMENT_RETURN_ROUTE = /^\/boutique\/commandes\/[A-Za-z0-9-]+\/paiement(?:-echoue)?$/;

// HelloAsso receives a URL without a fragment, pointing at the existing index.
// Before Angular starts, translate its return parameter into a hash route.
export function shopPaymentReturnUrl(baseURI: string, orderNumber: string, failed = false): string {
  const url = new URL(baseURI);
  url.hash = '';
  url.search = '';
  url.searchParams.set(PAYMENT_RETURN_PARAM,
    `/boutique/commandes/${encodeURIComponent(orderNumber)}/paiement${failed ? '-echoue' : ''}`);
  return url.href;
}

export function restoredShopPaymentUrl(href: string): string | null {
  const url = new URL(href);
  const route = url.searchParams.get(PAYMENT_RETURN_PARAM);
  if (!route || !PAYMENT_RETURN_ROUTE.test(route)) return null;
  url.searchParams.delete(PAYMENT_RETURN_PARAM);
  url.hash = route;
  return url.href;
}
