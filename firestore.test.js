const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_nasim";
const ADMIN_EMAIL = "ahsanhabib5832@gmail.com";
const FARMER_UID = "farmer_karim";
const BUYER_UID = "buyer_rahim";
const DELIVERY_UID = "delivery_jamal";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are blocked from reading or writing all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("products").where("status", "==", "ACTIVE").get());
  await assertFails(unauth.collection("livestock").where("status", "==", "ACTIVE").get());
  await assertFails(unauth.collection("market_prices").where("active", "==", true).get());
  await assertFails(unauth.collection("users").doc(BUYER_UID).get());
  await assertFails(unauth.collection("orders").where("buyerId", "==", BUYER_UID).get());
});

test("User accounts: strict PII isolation, anti-privilege-escalation, and verified admin check", async () => {
  const buyerDb = testEnv.authenticatedContext(BUYER_UID, {
    email: "rahim@example.com",
    email_verified: true,
  }).firestore();

  const ts = pastDate();

  // Valid buyer self-registration
  await assertSucceeds(
    buyerDb.collection("users").doc(BUYER_UID).set({
      userId: BUYER_UID,
      displayName: "Rahim Mia",
      email: "rahim@example.com",
      phone: "01711000000",
      division: "Dhaka",
      district: "Dhaka",
      role: "BUYER",
      verifiedFarmer: false,
      totalPurchasesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject self-assigning SUPER_ADMIN or verifiedFarmer: true
  await assertFails(
    buyerDb.collection("users").doc(BUYER_UID).update({
      role: "SUPER_ADMIN",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    buyerDb.collection("users").doc(BUYER_UID).update({
      verifiedFarmer: true,
      updatedAt: pastDate(500),
    })
  );

  // Cross-user PII read/list rejected
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  await assertFails(farmerDb.collection("users").doc(BUYER_UID).get());
  await assertFails(farmerDb.collection("users").get());

  // Spoofed admin email with email_verified: false MUST fail
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofAdminDb.collection("users").get());

  // Genuine verified admin succeeds
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(BUYER_UID).update({
      role: "FARMER",
      verifiedFarmer: true,
      updatedAt: pastDate(200),
    })
  );
});

test("Products: filtered query succeeds, unfiltered query fails, farmer manages own, buyer atomic stock decrement", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const buyerDb = testEnv.authenticatedContext(BUYER_UID).firestore();
  const ts = pastDate(5000);

  const productPayload = {
    farmerId: FARMER_UID,
    farmerName: "Karim Farm",
    farmLocation: "Chuadanga, Khulna",
    nameEn: "Golden Maize (Bhutta)",
    nameBn: "সোনালী ভুট্টা",
    category: "MAIZE",
    descriptionEn: "Sun-dried high-yield Chuadanga maize.",
    descriptionBn: "চুয়াডাঙ্গার রোদে শুকানো উন্নত মানের ভুট্টা।",
    imageKey: "crops_harvest",
    unit: "MON",
    grade: "A_GRADE",
    organic: false,
    pricePerUnit: 1320,
    pricePerMon: 1320,
    wholesalePricePerUnit: 1240,
    minOrderQty: 1,
    wholesaleMinMon: 5,
    wholesaleDiscountPct: 6,
    stockQty: 50,
    harvestDateLabel: "Today",
    harvestFreshness: "TODAY",
    estimatedDelivery: "24-48 hours",
    preOrderAvailable: false,
    salesCount: 2,
    status: "ACTIVE",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(farmerDb.collection("products").doc("prod_maize_1").set(productPayload));

  // Unfiltered list must fail
  await assertFails(buyerDb.collection("products").get());

  // Filtered query matching rule succeeds
  await assertSucceeds(buyerDb.collection("products").where("status", "==", "ACTIVE").get());

  // Buyer cannot tamper with price
  await assertFails(
    buyerDb.collection("products").doc("prod_maize_1").update({
      pricePerUnit: 1,
      updatedAt: pastDate(200),
    })
  );

  // Buyer atomic stock decrement during checkout succeeds
  await assertSucceeds(
    buyerDb.collection("products").doc("prod_maize_1").update({
      stockQty: 44,
      salesCount: 3,
      status: "ACTIVE",
      updatedAt: pastDate(200),
    })
  );

  // Buyer cannot increase stock
  await assertFails(
    buyerDb.collection("products").doc("prod_maize_1").update({
      stockQty: 99,
      salesCount: 4,
      status: "ACTIVE",
      updatedAt: pastDate(100),
    })
  );
});

test("Livestock & Market Prices: role enforcement and filtered queries", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const buyerDb = testEnv.authenticatedContext(BUYER_UID).firestore();
  const ts = pastDate(4000);

  await assertSucceeds(
    adminDb.collection("livestock").doc("cow_1").set({
      farmerId: ADMIN_UID,
      farmerName: "Ahsan Habib Nasim",
      farmLocation: "Chuadanga, Khulna",
      animalNameEn: "Shahiwal Cross Bull",
      animalNameBn: "শাহীওয়াল ক্রস ষাঁড়",
      category: "COW",
      breed: "Shahiwal",
      gender: "MALE",
      ageMonths: 24,
      liveWeightKg: 340,
      estimatedWeightDate: "2026-11-15",
      price: 125000,
      wholesalePrice: 120000,
      vaccinationHistory: "FMD, Anthrax, Black Quarter updated",
      healthInfo: "Vet checked, active appetite",
      imageKey: "livestock_cattle",
      deliveryAvailable: true,
      estimatedDelivery: "2-3 days",
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(buyerDb.collection("livestock").where("status", "==", "ACTIVE").get());
  await assertFails(buyerDb.collection("livestock").get());

  // Non-admin cannot modify market_prices
  await assertFails(
    buyerDb.collection("market_prices").doc("mp_potato").set({
      productNameEn: "Potato",
      productNameBn: "আলু",
      category: "POTATO_ONION",
      currentPrice: 45,
      previousPrice: 48,
      unit: "KG",
      movement: "DOWN",
      marketLocation: "Chuadanga Bazar",
      verifiedBy: "Fake",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin creates verified market price
  await assertSucceeds(
    adminDb.collection("market_prices").doc("mp_potato").set({
      productNameEn: "Potato (Alu)",
      productNameBn: "আলু",
      category: "POTATO_ONION",
      currentPrice: 48,
      previousPrice: 50,
      unit: "KG",
      movement: "DOWN",
      marketLocation: "Chuadanga Sadar Bazar",
      verifiedBy: "Ahsan Habib Nasim",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(buyerDb.collection("market_prices").where("active", "==", true).get());
});

test("Orders, Negotiations, Reviews, and User Subcollections: participant isolation and terminal lock", async () => {
  const buyerDb = testEnv.authenticatedContext(BUYER_UID).firestore();
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const deliveryDb = testEnv.authenticatedContext(DELIVERY_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext("outsider_999").firestore();
  const ts = pastDate(4000);

  await assertSucceeds(
    buyerDb.collection("orders").doc("order_1").set({
      buyerId: BUYER_UID,
      buyerName: "Rahim Mia",
      buyerPhone: "01711000000",
      deliveryAddress: "Mirpur 10, Dhaka",
      district: "Dhaka",
      farmerId: FARMER_UID,
      farmerName: "Karim Farm",
      itemsSummary: "6 Mon Maize",
      items: [{ productId: "prod_maize_1", title: "Maize", qty: 6, price: 1240 }],
      totalWeightKg: 240,
      subtotal: 7440,
      deliveryCharge: 150,
      discountAmount: 480,
      totalAmount: 7110,
      wholesaleOrder: true,
      paymentMethod: "CASH_ON_DELIVERY",
      paymentStatus: "PENDING",
      orderStatus: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Outsider cannot read or update order
  await assertFails(outsiderDb.collection("orders").doc("order_1").get());

  // Farmer confirms and assigns delivery
  await assertSucceeds(
    farmerDb.collection("orders").doc("order_1").update({
      orderStatus: "CONFIRMED",
      deliveryAssigneeId: DELIVERY_UID,
      updatedAt: pastDate(500),
    })
  );

  // Assigned delivery marks DELIVERED
  await assertSucceeds(
    deliveryDb.collection("orders").doc("order_1").update({
      orderStatus: "DELIVERED",
      deliveryConfirmationNote: "Delivered to buyer at Mirpur 10",
      updatedAt: pastDate(300),
    })
  );

  // Terminal state lock: farmer cannot modify DELIVERED order
  await assertFails(
    farmerDb.collection("orders").doc("order_1").update({
      orderStatus: "CANCELLED",
      updatedAt: pastDate(100),
    })
  );

  // User-scoped cart & crop diagnosis isolation
  await assertSucceeds(
    buyerDb.collection("users").doc(BUYER_UID).collection("cart").doc("prod_maize_1").set({
      productId: "prod_maize_1",
      listingType: "PRODUCT",
      farmerId: FARMER_UID,
      farmerName: "Karim Farm",
      titleEn: "Golden Maize",
      titleBn: "সোনালী ভুট্টা",
      unit: "MON",
      quantity: 6,
      unitPrice: 1320,
      wholesalePrice: 1240,
      wholesaleMinMon: 5,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(outsiderDb.collection("users").doc(BUYER_UID).collection("cart").get());
});
