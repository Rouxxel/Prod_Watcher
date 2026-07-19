DROP VIEW IF EXISTS public.product_stock_summary;
DROP VIEW IF EXISTS public.inventory_balances;

CREATE VIEW public.inventory_balances AS
SELECT sm.ecosystem_id,sm.product_id,sm.warehouse_id,SUM(sm.delta)::integer quantity FROM (
 SELECT ecosystem_id,product_id,to_warehouse_id warehouse_id,qty delta FROM public.stock_movements WHERE type='IN'
 UNION ALL SELECT ecosystem_id,product_id,from_warehouse_id,-qty FROM public.stock_movements WHERE type='OUT'
 UNION ALL SELECT ecosystem_id,product_id,from_warehouse_id,-qty FROM public.stock_movements WHERE type='TRANSFER'
 UNION ALL SELECT ecosystem_id,product_id,to_warehouse_id,qty FROM public.stock_movements WHERE type='TRANSFER'
 UNION ALL SELECT ecosystem_id,product_id,from_warehouse_id,-qty FROM public.stock_movements WHERE type='ADJUSTMENT' AND from_warehouse_id IS NOT NULL
 UNION ALL SELECT ecosystem_id,product_id,to_warehouse_id,qty FROM public.stock_movements WHERE type='ADJUSTMENT' AND to_warehouse_id IS NOT NULL) sm GROUP BY sm.ecosystem_id,sm.product_id,sm.warehouse_id;
CREATE VIEW public.product_stock_summary AS SELECT p.id product_id,p.default_warehouse_id warehouse_id,COALESCE(ib.quantity,0)::integer quantity FROM public.products p LEFT JOIN public.inventory_balances ib ON ib.product_id=p.id AND ib.warehouse_id=p.default_warehouse_id AND ib.ecosystem_id=p.ecosystem_id;
GRANT SELECT ON public.inventory_balances, public.product_stock_summary TO authenticated, service_role;
