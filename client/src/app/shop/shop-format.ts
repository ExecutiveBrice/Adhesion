import { ShopMoneyDto, ShopOrderDto } from './models/shop.models';

const orderStatusLabels: Record<ShopOrderDto['status'], string> = {
  DRAFT: 'Brouillon',
  PENDING_PAYMENT: 'En attente de paiement',
  PAID: 'Payée',
  PROCESSING: 'En préparation',
  COMPLETED: 'Terminée',
  CANCELLED: 'Annulée',
  EXPIRED: 'Expirée',
  REFUNDED: 'Remboursée'
};

export function formatShopOrderStatus(status: ShopOrderDto['status']): string {
  return orderStatusLabels[status];
}

export function formatShopMoney(money: ShopMoneyDto): string {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: money.currency }).format(money.amountInCents / 100);
}

export function formatShopArticleMoney(money: ShopMoneyDto): string {
  if (money.amountInCents % 100 !== 0) {
    return formatShopMoney(money);
  }

  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: money.currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(money.amountInCents / 100);
}
