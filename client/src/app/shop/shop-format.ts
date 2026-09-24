import { ShopMoneyDto } from './models/shop.models';

export function formatShopMoney(money: ShopMoneyDto): string {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: money.currency }).format(money.amountInCents / 100);
}
