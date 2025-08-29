# Razorpay Integration Setup

## Required Environment Variables

You need both the **Key ID** (public) and **Key Secret** (private) from your Razorpay dashboard:

### For Development:
```bash
export RAZORPAY_KEY_ID=rzp_test_your_key_id_here
export RAZORPAY_KEY_SECRET=your_secret_key_here
```

### For Production (Render/Deployment):
Set these as environment variables in your deployment platform:

1. **RAZORPAY_KEY_ID** = `rzp_live_your_live_key_id` (for production) or `rzp_test_your_test_key_id` (for testing)
2. **RAZORPAY_KEY_SECRET** = Your secret key (NEVER expose this publicly)

## How to Get Razorpay Keys:

1. **Sign up** at [https://razorpay.com](https://razorpay.com)
2. **Complete KYC** verification 
3. **Go to Settings > API Keys**
4. **Generate Test Keys** (for development) or **Live Keys** (for production)
5. **Copy both Key ID and Secret**

## Security Notes:

- ✅ **Key ID** can be exposed in frontend (checkout.html uses it)
- ❌ **Key Secret** must NEVER be in frontend code or public repos
- ✅ **Key Secret** is only used on server-side (RazorpayService.java)

## Payment Flow:

1. User clicks "Pay with Razorpay" → calls `/create-order`
2. Server creates Razorpay order using **secret key**
3. Frontend opens Razorpay checkout with **public key**
4. User completes payment → Razorpay sends response
5. Frontend calls `/verify-payment` with payment details
6. Server verifies payment signature using **secret key**
7. If valid → create user orders and clear cart

## Fallback:

The old QR code payment method is still available as a fallback option.
