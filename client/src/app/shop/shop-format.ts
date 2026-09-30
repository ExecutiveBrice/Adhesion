import { ShopMoneyDto } from './models/shop.models';

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
