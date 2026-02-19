package com.ecotrace.app.data.repository

import com.ecotrace.app.data.models.ProductInfo
import com.ecotrace.app.data.models.ScannedProduct
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val scannedProductDao: ScannedProductDao,
    private val productInfoDao: ProductInfoDao
) {
    fun getAllScannedProductsFlow(): Flow<List<ScannedProduct>> = 
        scannedProductDao.getAllFlow()

    fun getScannedProductsForMonth(year: Int, month: Int): Flow<List<ScannedProduct>> {
        val ym = YearMonth.of(year, month)
        val from = ym.atDay(1).toEpochDay()
        val to = ym.atEndOfMonth().toEpochDay()
        return scannedProductDao.getByPeriodFlow(from, to)
    }

    suspend fun addScannedProduct(
        barcode: String,
        name: String,
        brand: String,
        category: String,
        kgCo2ePer100g: Double,
        weight: Double,
        imageUrl: String = ""
    ) {
        val product = ScannedProduct(
            id = java.util.UUID.randomUUID().toString(),
            barcode = barcode,
            name = name,
            brand = brand,
            category = category,
            kgCo2ePer100g = kgCo2ePer100g,
            weight = weight,
            date = LocalDate.now().toEpochDay(),
            imageUrl = imageUrl
        )
        scannedProductDao.insert(product)
    }

    suspend fun deleteScannedProduct(id: String) = scannedProductDao.deleteById(id)

    suspend fun getProductInfoByBarcode(barcode: String): ProductInfo? =
        productInfoDao.getByBarcode(barcode)

    suspend fun saveProductInfo(productInfo: ProductInfo) =
        productInfoDao.insert(productInfo)

    suspend fun getProductDatabaseCount(): Int = productInfoDao.getCount()

    fun getDefaultProductDatabase(): List<ProductInfo> = listOf(
        // Chocolat & Confiserie (15 produits)
        ProductInfo("3017620422003", "Nutella", "Ferrero", "Pâte à tartiner", 5.3),
        ProductInfo("3017624010701", "Kinder Bueno", "Ferrero", "Chocolat", 4.8),
        ProductInfo("3017620425004", "Kinder Surprise", "Ferrero", "Chocolat", 5.1),
        ProductInfo("3017620429019", "Ferrero Rocher", "Ferrero", "Chocolat", 5.6),
        ProductInfo("3017620425707", "Kinder Chocolat", "Ferrero", "Chocolat", 4.9),
        ProductInfo("7622210449283", "Milka Lait", "Milka", "Chocolat", 5.2),
        ProductInfo("7622300489434", "Toblerone", "Toblerone", "Chocolat", 5.4),
        ProductInfo("3045320073188", "Nesquik Poudre", "Nestlé", "Chocolat en poudre", 2.8),
        ProductInfo("7613034626844", "Lindt Excellence", "Lindt", "Chocolat", 5.8),
        ProductInfo("3229820129488", "Côte d'Or Lait", "Côte d'Or", "Chocolat", 5.1),
        ProductInfo("8000500310427", "Tic Tac Menthe", "Ferrero", "Bonbon", 2.1),
        ProductInfo("3168930009559", "Haribo Dragibus", "Haribo", "Bonbon", 2.3),
        ProductInfo("3168930009566", "Haribo Fraises Tagada", "Haribo", "Bonbon", 2.2),
        ProductInfo("3017620401305", "Mon Chéri", "Ferrero", "Chocolat", 5.3),
        ProductInfo("40111421", "M&M's Peanut", "Mars", "Chocolat", 4.7),
        
        // Boissons (20 produits)
        ProductInfo("3168930010265", "Coca-Cola 1.5L", "Coca-Cola", "Boisson", 0.33),
        ProductInfo("3168930009887", "Fanta Orange", "Coca-Cola", "Boisson", 0.35),
        ProductInfo("3168930005698", "Sprite", "Coca-Cola", "Boisson", 0.31),
        ProductInfo("5449000000996", "Coca-Cola Zero", "Coca-Cola", "Boisson", 0.32),
        ProductInfo("3124480186706", "Orangina", "Orangina", "Boisson", 0.36),
        ProductInfo("3124480191106", "Schweppes Tonic", "Schweppes", "Boisson", 0.34),
        ProductInfo("3168930009870", "Fanta Citron", "Coca-Cola", "Boisson", 0.35),
        ProductInfo("3124480191205", "Schweppes Agrumes", "Schweppes", "Boisson", 0.34),
        ProductInfo("3029330003533", "Evian 1.5L", "Evian", "Eau", 0.08),
        ProductInfo("3029330003915", "Badoit 1L", "Badoit", "Eau gazeuse", 0.15),
        ProductInfo("3274080005003", "Perrier 1L", "Perrier", "Eau gazeuse", 0.14),
        ProductInfo("3274080011004", "Vittel 1.5L", "Vittel", "Eau", 0.09),
        ProductInfo("3089600002003", "Contrex 1.5L", "Contrex", "Eau", 0.09),
        ProductInfo("3168930009894", "Minute Maid Orange", "Coca-Cola", "Jus de fruit", 0.52),
        ProductInfo("3124480191304", "Oasis Tropical", "Oasis", "Boisson aux fruits", 0.48),
        ProductInfo("3124480191403", "Oasis Pomme Cassis", "Oasis", "Boisson aux fruits", 0.47),
        ProductInfo("3045320001433", "Nescafé Classic", "Nestlé", "Café", 4.5),
        ProductInfo("3045320001440", "Nescafé Gold", "Nestlé", "Café", 4.6),
        ProductInfo("8712100325021", "Lipton Ice Tea Pêche", "Lipton", "Thé glacé", 0.38),
        ProductInfo("3083681810509", "Tropicana Orange", "Tropicana", "Jus de fruit", 0.55),
        
        // Produits laitiers & Fromages (25 produits)
        ProductInfo("3270190207238", "Président Beurre", "Président", "Produit laitier", 8.9),
        ProductInfo("3228857000906", "Emmental Président", "Président", "Fromage", 9.8),
        ProductInfo("3270160517084", "Président Camembert", "Président", "Fromage", 10.5),
        ProductInfo("3250391805778", "Danone Activia", "Danone", "Yaourt", 1.2),
        ProductInfo("3250392409012", "Danone Nature", "Danone", "Yaourt", 1.1),
        ProductInfo("3250391675685", "Actimel", "Danone", "Yaourt à boire", 1.4),
        ProductInfo("3033710065912", "Yoplait Nature", "Yoplait", "Yaourt", 1.1),
        ProductInfo("3033710074914", "Yoplait Fruits", "Yoplait", "Yaourt", 1.3),
        ProductInfo("3023290631010", "La Laitière Crème Dessert", "La Laitière", "Dessert", 1.8),
        ProductInfo("3023290631027", "La Laitière Riz au Lait", "La Laitière", "Dessert", 1.6),
        ProductInfo("3270160517091", "Président Brie", "Président", "Fromage", 10.2),
        ProductInfo("3270160517107", "Président Roquefort", "Président", "Fromage", 11.5),
        ProductInfo("3228857000913", "Comté Président", "Président", "Fromage", 10.8),
        ProductInfo("3228857000920", "Gruyère Président", "Président", "Fromage", 10.6),
        ProductInfo("3270190207245", "Président Crème Fraîche", "Président", "Produit laitier", 3.2),
        ProductInfo("3250391804009", "Danone Danette Chocolat", "Danone", "Dessert", 1.5),
        ProductInfo("3250391804016", "Danone Danette Vanille", "Danone", "Dessert", 1.4),
        ProductInfo("3033710074921", "Panier de Yoplait", "Yoplait", "Yaourt", 1.4),
        ProductInfo("3250391675692", "Actimel Fruits", "Danone", "Yaourt à boire", 1.5),
        ProductInfo("3270160517114", "Président Chèvre", "Président", "Fromage", 9.5),
        ProductInfo("3228857000937", "Mozzarella Président", "Président", "Fromage", 8.2),
        ProductInfo("3270190207252", "Président Lait Demi-Écrémé", "Président", "Lait", 1.3),
        ProductInfo("3270190207269", "Président Lait Entier", "Président", "Lait", 1.4),
        ProductInfo("3250391675708", "Danone Velouté Fruits", "Danone", "Yaourt", 1.3),
        ProductInfo("3017760000000", "Carte d'Or Vanille", "Carte d'Or", "Glace", 3.2),
        
        // Charcuterie (12 produits)
        ProductInfo("3560070462971", "Herta Jambon", "Herta", "Charcuterie", 4.2),
        ProductInfo("3560070724222", "Herta Knacki", "Herta", "Saucisse", 5.8),
        ProductInfo("3256220112233", "Fleury Michon Jambon", "Fleury Michon", "Charcuterie", 3.8),
        ProductInfo("3560070462988", "Herta Lardons", "Herta", "Charcuterie", 5.2),
        ProductInfo("3560070462995", "Herta Saucisson", "Herta", "Charcuterie", 6.1),
        ProductInfo("3256220112240", "Fleury Michon Blanc de Poulet", "Fleury Michon", "Charcuterie", 3.2),
        ProductInfo("3256220112257", "Fleury Michon Saumon Fumé", "Fleury Michon", "Poisson", 5.8),
        ProductInfo("3560070724239", "Herta Cordon Bleu", "Herta", "Plat préparé", 4.5),
        ProductInfo("3560070724246", "Herta Nuggets", "Herta", "Plat préparé", 4.8),
        ProductInfo("3256220112264", "Fleury Michon Surimi", "Fleury Michon", "Poisson", 3.5),
        ProductInfo("3560070462964", "Herta Pâté", "Herta", "Charcuterie", 5.5),
        ProductInfo("3256220112271", "Fleury Michon Rillettes", "Fleury Michon", "Charcuterie", 6.2),
        
        // Pâtes, Riz & Céréales (15 produits)
        ProductInfo("8076809513203", "Barilla Pâtes Spaghetti", "Barilla", "Pâtes", 1.1),
        ProductInfo("3083680085175", "Panzani Pâtes", "Panzani", "Pâtes", 1.0),
        ProductInfo("8076809513210", "Barilla Penne", "Barilla", "Pâtes", 1.1),
        ProductInfo("8076809513227", "Barilla Fusilli", "Barilla", "Pâtes", 1.1),
        ProductInfo("3083680085182", "Panzani Coquillettes", "Panzani", "Pâtes", 1.0),
        ProductInfo("3083680085199", "Panzani Tagliatelles", "Panzani", "Pâtes", 1.0),
        ProductInfo("3228857000944", "Riz Uncle Ben's", "Uncle Ben's", "Riz", 2.1),
        ProductInfo("3228857000951", "Riz Basmati Taureau Ailé", "Taureau Ailé", "Riz", 2.0),
        ProductInfo("3228857000968", "Riz Complet Uncle Ben's", "Uncle Ben's", "Riz", 2.2),
        ProductInfo("7613033489427", "Corn Flakes Kellogg's", "Kellogg's", "Céréales", 1.8),
        ProductInfo("7613033489434", "Special K Kellogg's", "Kellogg's", "Céréales", 1.7),
        ProductInfo("7613287072009", "Chocapic Nestlé", "Nestlé", "Céréales", 2.3),
        ProductInfo("7613287072016", "Fitness Nestlé", "Nestlé", "Céréales", 1.9),
        ProductInfo("3228857000975", "Ebly Blé", "Ebly", "Céréales", 1.5),
        ProductInfo("3083680085206", "Panzani Couscous", "Panzani", "Semoule", 1.3),
        
        // Biscuits & Gâteaux (15 produits)
        ProductInfo("3760074380534", "Michel et Augustin Cookies", "Michel et Augustin", "Biscuit", 2.9),
        ProductInfo("7622210449290", "LU Petit Beurre", "LU", "Biscuit", 2.1),
        ProductInfo("7622210449306", "LU Prince", "LU", "Biscuit", 2.8),
        ProductInfo("7622210449313", "LU Pépito", "LU", "Biscuit", 3.1),
        ProductInfo("7622210449320", "LU Petit Écolier", "LU", "Biscuit", 3.2),
        ProductInfo("7622210449337", "LU BN", "LU", "Biscuit", 2.9),
        ProductInfo("3017620422010", "Kinder Country", "Ferrero", "Biscuit", 4.2),
        ProductInfo("3017620422027", "Kinder Délice", "Ferrero", "Gâteau", 3.8),
        ProductInfo("7622210449344", "Oreo", "Oreo", "Biscuit", 2.7),
        ProductInfo("7622210449351", "Granola", "LU", "Biscuit", 2.4),
        ProductInfo("3017620422034", "Bonne Maman Madeleines", "Bonne Maman", "Gâteau", 2.6),
        ProductInfo("3017620422041", "St Michel Madeleine", "St Michel", "Gâteau", 2.5),
        ProductInfo("7622210449368", "Tuc", "LU", "Biscuit apéritif", 2.3),
        ProductInfo("7622210449375", "Belin Monaco", "Belin", "Biscuit apéritif", 2.2),
        ProductInfo("7622210449382", "Curly Belin", "Belin", "Biscuit apéritif", 2.4),
        
        // Conserves & Plats préparés (10 produits)
        ProductInfo("3083681810516", "Cassegrain Petits Pois", "Cassegrain", "Conserve", 0.8),
        ProductInfo("3083681810523", "Cassegrain Haricots Verts", "Cassegrain", "Conserve", 0.7),
        ProductInfo("3083681810530", "Bonduelle Maïs", "Bonduelle", "Conserve", 0.9),
        ProductInfo("3083681810547", "Bonduelle Flageolets", "Bonduelle", "Conserve", 1.1),
        ProductInfo("3083681810554", "William Saurin Cassoulet", "William Saurin", "Plat préparé", 3.2),
        ProductInfo("3083681810561", "William Saurin Petit Salé", "William Saurin", "Plat préparé", 3.5),
        ProductInfo("3083681810578", "Panzani Sauce Tomate", "Panzani", "Sauce", 1.2),
        ProductInfo("3083681810585", "Panzani Sauce Bolognaise", "Panzani", "Sauce", 2.8),
        ProductInfo("3083681810592", "Amora Ketchup", "Amora", "Sauce", 0.9),
        ProductInfo("3083681810608", "Amora Mayonnaise", "Amora", "Sauce", 3.1)
    )
}
