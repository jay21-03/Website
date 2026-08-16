/** @typedef {'NEW'|'CONFIRMED'|'COMPLETED'|'CANCELLED'} OrderStatus */
/** @typedef {'PENDING'|'PAID'|'FAILED'|'CANCELLED'|'EXPIRED'|'REFUNDED'} PaymentStatus */

/** @typedef {{receiverName:string,phone:string,email:string,address:string,note:string}} CheckoutRequest */
/** @typedef {{checkoutOperationId:number,orderId:number,orderCode:string,paymentId:number,paymentStatus:PaymentStatus,totalAmount:number,checkoutUrl:string|null,qrCode:string|null,expiresAt:string|null}} CheckoutResponse */
/** @typedef {{productId:number,productNameVi:string,productNameEn:string,basePrice:number,sellingPrice:number,quantity:number,totalPrice:number}} OrderItem */
/** @typedef {{id:number,orderCode:string,createdAt:string,totalAmount:number,orderStatus:OrderStatus,paymentStatus:PaymentStatus}} OrderSummary */
/** @typedef {OrderSummary & {userId:number,receiverName:string,phone:string,email:string|null,address:string,note:string|null,items:OrderItem[],subtotal:number,updatedAt:string}} OrderDetail */
/** @template T @typedef {{content:T[],page:number,size:number,totalElements:number,totalPages:number,first:boolean,last:boolean}} PageResponse */
/** @typedef {{field:string,message:string}} FieldError */
/** @typedef {{code:string,message:string,fieldErrors:FieldError[]}} ApiErrorPayload */

export {}
