export interface ModalConfig {
    modalTitle: string
    modalBody:string
    dismissButtonLabel?: string
    closeButtonLabel?: string
    acceptButtonLabel?: string   
    shouldClose?(): Promise<boolean> | boolean
    shouldDismiss?(): Promise<boolean> | boolean
    onClose?(): Promise<boolean> | boolean
    onDismiss?(): Promise<boolean> | boolean
    shouldAccept?(): Promise<boolean> | boolean
    onAccept?(): Promise<boolean> | boolean
    disableCloseButton?: boolean
    disableDismissButton?: boolean
    hideCloseButton?: boolean
    hideDismissButton?: boolean
    disableAcceptButton?: boolean
    hideAcceptButton?: boolean
}