import { ShopAdminOrderDto, ShopAdminProductDto } from './shop.models';

export interface ShopItemAvailability {
  state: 'ready' | 'missing' | 'unknown' | 'completed' | 'cancelled';
  label: string;
  missingQuantity: number;
}

/** Simulation de remise : commandes payées prioritaires, puis ordre de création.
 * Le stock réservé des commandes payées a déjà été déduit du stock comptable. */
export function buildOrderAvailability(products: ShopAdminProductDto[], orders: ShopAdminOrderDto[]): Map<number, ShopItemAvailability> {
  const variants = new Map(products.flatMap(product => product.variants.map(variant => [variant.id, variant] as const)));
  const remaining = new Map<number, number>();
  for (const variant of variants.values()) {
    if (variant.stockTracked && variant.stockOnHand != null) remaining.set(variant.id, variant.stockOnHand);
  }
  const openOrders = orders.filter(order => ['PAID', 'PROCESSING', 'PENDING_PAYMENT'].includes(order.status));
  for (const order of openOrders) {
    if (order.status === 'PENDING_PAYMENT') continue;
    for (const item of order.items) {
      if (item.status === 'COMPLETED' || item.status === 'CANCELLED' || item.stockReserved === false) continue;
      if (item.productVariantId != null && remaining.has(item.productVariantId)) {
        remaining.set(item.productVariantId, remaining.get(item.productVariantId)! + item.quantity);
      }
    }
  }
  for (const [id, quantity] of remaining) remaining.set(id, Math.max(0, quantity));
  openOrders.sort((a, b) => Number(a.status === 'PENDING_PAYMENT') - Number(b.status === 'PENDING_PAYMENT')
    || Date.parse(a.createdAt) - Date.parse(b.createdAt) || a.orderNumber.localeCompare(b.orderNumber));
  const result = new Map<number, ShopItemAvailability>();
  for (const order of openOrders) {
    for (const item of [...order.items].sort((a, b) => a.id - b.id)) {
      if (item.status === 'COMPLETED' || item.status === 'CANCELLED') continue;
      const id = item.productVariantId;
      const variant = id == null ? undefined : variants.get(id);
      if (id == null || !remaining.has(id)) {
        result.set(item.id, { state: 'unknown', label: variant && !variant.stockTracked ? 'Stock non suivi' : 'Stock à vérifier', missingQuantity: 0 });
        continue;
      }
      const available = Math.min(item.quantity, remaining.get(id)!);
      remaining.set(id, remaining.get(id)! - available);
      const missingQuantity = item.quantity - available;
      result.set(item.id, { state: missingQuantity ? 'missing' : 'ready', missingQuantity,
        label: missingQuantity ? `Manque ${missingQuantity} sur ${item.quantity}` : `Disponible : ${item.quantity} / ${item.quantity}` });
    }
  }
  return result;
}
