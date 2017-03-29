/*
 * Select all closure data needed to build a tree representation
 * This example selects node A, and all the required child data
 */
select
  n.parent_node_id, c.child_node_id, n.name
from
  eas_closure c
inner join
  eas_node n
on
  c.child_node_id = n.node_id
where
  c.parent_node_id = 1
order by
  c.depth, n.name

/*
 * Select tree path, from leaf node (J) to root node (A), i.e. J->A
 */
select
  p.parent_node_id, n.name, p.depth
from
  eas_closure p
join
  eas_node n
on
  n.node_id = p.parent_node_id
where
  p.child_node_id = 10
order by
  p.depth desc

/*
 * 
 */  
select
	distinct l.link_id, l.parent_node_id, l.child_node_id, l.depth
from
	eas_closure p
inner join
	eas_closure l
on
	p.parent_node_id = l.parent_node_id
inner join
	eas_closure c
on
	c.child_node_id = l.child_node_id
inner join
	eas_closure to_delete
on
	p.child_node_id = to_delete.parent_node_id
	and c.parent_node_id = to_delete.child_node_id
	and to_delete.depth < 2
inner join
(
	select c.child_node_id as node_to_delete
	from eas_closure c
	inner join fs_node n
	on c.child_node_id = n.node_id
	where c.parent_node_id = 4  
) deleteTable
on
(
	to_delete.parent_node_id = deleteTable.node_to_delete
	or
	to_delete.child_node_id = deleteTable.node_to_delete
)
order by
	l.parent_node_id, l.child_node_id, l.depth
  
/*
 * example query to delete node I (number 9)
 */
delete
	eas_closure
where link_id in (
	select
		distinct l.link_id
	from
		eas_closure p
	inner join
		eas_closure l
	on
		p.parent_node_id = l.parent_node_id
	inner join
		eas_closure c
	on
		c.child_node_id = l.child_node_id
	inner join
		eas_closure to_delete
	on
		p.child_node_id = to_delete.parent_node_id
		and c.parent_node_id = to_delete.child_node_id
		and to_delete.depth < 2
	inner join
	(
		select c.child_node_id as node_to_delete
		from eas_closure c
		inner join fs_node n
		on c.child_node_id = n.node_id
		where c.parent_node_id = 9  
	) deleteTable
	on
	(
		to_delete.parent_node_id = deleteTable.node_to_delete
		or
		to_delete.child_node_id = deleteTable.node_to_delete
	)
	order by
		l.parent_node_id, l.child_node_id, l.depth
)