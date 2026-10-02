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
  imageUrl: string | null;
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
  version: number;
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

export interface ShopAdminVariantCreateRequest {
  sku: string;
  label: string | null;
  priceAmountInCents: number;
  currency: string;
  active: boolean;
  displayOrder: number;
}

export interface ShopAdminVariantRequest extends ShopAdminVariantCreateRequest {
  stockTracked: boolean;
  expectedVersion?: number;
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
  status: 'DRAFT' | 'PENDING_PAYMENT' | 'PAID' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED' | 'REFUNDED';
  items: ShopOrderItemDto[];
  subtotal: ShopMoneyDto;
  discountTotal: ShopMoneyDto;
  feesTotal: ShopMoneyDto;
  total: ShopMoneyDto;
  refundRequested: boolean;
  createdAt: string;
  paymentExpiresAt?: string | null;
}

export interface ShopAdminOrderDto {
  orderNumber: string;
  status: ShopOrderDto['status'];
  total: ShopMoneyDto;
  createdAt: string;
  paymentExpiresAt?: string | null;
  customerEmail: string | null;
  customerTribeId: number | null;
  refundRequested: boolean;
  items: ShopAdminOrderItemDto[];
}

export type ShopOrderMessageSender = 'CUSTOMER' | 'SHOP_MANAGER';

export interface ShopOrderMessageDto {
  id: number;
  senderType: ShopOrderMessageSender;
  content: string;
  createdAt: string;
}

export type ShopAdminOrderItemStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED';

export interface ShopAdminOrderItemDto {
  id: number;
  productVariantId: number | null;
  productName: string;
  variantName: string | null;
  sku: string | null;
  unitPrice: ShopMoneyDto;
  quantity: number;
  lineTotal: ShopMoneyDto;
  status: ShopAdminOrderItemStatus;
  stockReserved?: boolean;
}

export interface ShopSupplierOrderDto {
  id: number;
  supplierName: string;
  reference: string | null;
  invoiceReference?: string | null;
  status: 'DRAFT' | 'ORDERED' | 'RECEIVED';
  createdAt: string;
  receivedAt: string | null;
  lines: ShopSupplierOrderLineDto[];
}

export interface ShopSupplierOrderLineDto {
  id: number;
  variantId: number;
  productName: string;
  variantName: string | null;
  sku: string;
  quantity: number;
  unitCostAmountInCents?: number | null;
  unitCostCurrency?: string | null;
  lineTotalAmountInCents?: number | null;
  expectedNeed?: number | null;
  extraApproved?: boolean;
}

export interface ShopSupplierOrderCreateRequest {
  supplierName: string;
  reference: string | null;
  lines: { variantId: number; quantity: number }[];
}

export interface ShopSupplierOrderCompletionRequest {
  invoiceReference: string;
  lines: { variantId: number; unitCostAmountInCents: number }[];
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
