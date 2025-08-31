# EXACT FILES FOR MANUAL DEBUGGING

## 1. MAIN CONTROLLER (FIXED VERSION)
File: src/main/java/com/example/EcommerceFresh/Controller/CheckOutController.java

Key method to examine (around line 373):
```java
@PostMapping("/checkout")
public String placeOrder(
        @RequestParam(required = false) Integer addressId,
        @RequestParam(required = false) String paymentMode,
        @RequestParam(required = false) String firstName,
        @RequestParam(required = false) String lastName,
        @RequestParam(required = false) String address1,
        @RequestParam(required = false) String address2,
        @RequestParam(required = false) String pinCode,
        @RequestParam(required = false) String town,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String additionalInfo,
         Principal principal) {
    // ... debug logs should appear here with [FIXED VERSION] prefix
```

## 2. CHECKOUT FORM 
File: src/main/resources/templates/checkout.html

Key form section (around line 74):
```html
<form id="checkoutForm" th:action="@{/checkout}" method="post">
    <!-- Form fields -->
    <input type="text" id="firstName" name="firstName" required>
    <input type="text" id="form14" name="address1" required>
    <input type="text" id="form18" name="phone" required>
    <input type="text" id="form16" name="pinCode" required>
    <!-- ... -->
</form>
```

## 3. DATABASE ENTITIES
Check these files for field definitions:
- src/main/java/com/example/EcommerceFresh/Entity/OrderGroup.java
- src/main/java/com/example/EcommerceFresh/Entity/UserOrder.java
- src/main/java/com/example/EcommerceFresh/Entity/Address.java

## 4. ADMIN TEMPLATES (WHERE ISSUE APPEARS)
Check these files to see how addresses are displayed:
- src/main/resources/templates/adminOrders.html
- src/main/resources/templates/adminOrderGroupView.html

## DEBUGGING COMMANDS TO RUN

### 1. Check if fixed code is deployed:
```bash
grep -n "FIXED VERSION" src/main/java/com/example/EcommerceFresh/Controller/CheckOutController.java
```

### 2. Check form action:
```bash
grep -n "action.*checkout" src/main/resources/templates/checkout.html
```

### 3. Check database structure:
```sql
DESCRIBE user_orders;
DESCRIBE order_groups;
DESCRIBE address;
```

### 4. Test order creation:
1. Place order as regular user
2. Check logs for "[FIXED VERSION]" messages
3. Check database immediately:
```sql
SELECT id, delivery_address, first_name, mobile 
FROM user_orders 
WHERE id = (SELECT MAX(id) FROM user_orders);
```

## IMMEDIATE ACTIONS

1. **First**: Confirm which version is running by checking for debug logs
2. **Second**: If old version, force redeploy or restart
3. **Third**: If new version but still broken, add more debug logs and test
4. **Fourth**: Use emergency SQL script as last resort

The key is to trace the data flow from form → controller → database → admin display.
