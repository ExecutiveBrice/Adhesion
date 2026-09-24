export interface ShopMoneyDto {
  amountInCents: number;
  currency: string;
}

export interface ShopCategoryDto {
  id: number;
  name: string;
  slug: string;
}

export interface ShopVariantDto {
  id: number;
  sku: string;
  label: string;
  price: ShopMoneyDto;
  available: boolean;
}

export interface ShopProductDto {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  categories: ShopCategoryDto[];
  variants: ShopVariantDto[];
}

export interface ShopAdminCategoryDto {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  active: boolean;
  displayOrder: number;
}

export interface ShopAdminVariantDto {
  id: number;
  sku: string;
  label: string | null;
  priceAmountInCents: number;
  currency: string;
  active: boolean;
  displayOrder: number;
  stockTracked: boolean;
  stockOnHand: number | null;
  stockReserved: number;
}

export interface ShopAdminProductDto {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  imageUrl: string | null;
  active: boolean;
  displayOrder: number;
  categories: ShopAdminCategoryDto[];
  variants: ShopAdminVariantDto[];
}

export interface ShopAdminProductRequest {
  name: string;
  slug: string;
  description: string | null;
  imageUrl: string | null;
  active: boolean;
  displayOrder: number;
  categoryIds: number[];
}

export interface ShopAdminCategoryRequest {
  name: string;
  slug: string;
  description: string | null;
  active: boolean;
  displayOrder: number;
}

export interface ShopAdminVariantRequest {
  sku: string;
  label: string | null;
  priceAmountInCents: number;
  currency: string;
  active: boolean;
  displayOrder: number;
  stockTracked: boolean;
  stockOnHand: number | null;
}

export interface CartItemRequestDto {
  variantId: number;
  quantity: number;
}

export interface CartLineDto {
  productId: number;
  productName: string;
  variantId: number;
  variantName: string;
  sku: string;
  unitPrice: ShopMoneyDto;
  quantity: number;
  lineTotal: ShopMoneyDto;
  available: boolean;
}

export interface CartQuoteDto {
  items: CartLineDto[];
  subtotal: ShopMoneyDto;
  discountTotal: ShopMoneyDto;
  feesTotal: ShopMoneyDto;
  total: ShopMoneyDto;
}

export interface ShopOrderItemDto {
  productId: number;
  productName: string;
  variantId: number;
  variantName: string;
  sku: string;
  unitPrice: ShopMoneyDto;
  quantity: number;
  lineTotal: ShopMoneyDto;
}

export interface ShopOrderDto {
  id: number;
  orderNumber: string;
  status: 'DRAFT' | 'PENDING_PAYMENT' | 'PAID' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED';
  items: ShopOrderItemDto[];
  subtotal: ShopMoneyDto;
  discountTotal: ShopMoneyDto;
  feesTotal: ShopMoneyDto;
  total: ShopMoneyDto;
  createdAt: string;
}

/** État local minimal : aucune somme ni prix n'est conservé comme référence de paiement. */
export interface CartEntry {
  variantId: number;
  quantity: number;
}

export interface PaymentSessionDto {
  redirectUrl: string | null;
  status: string;
}
