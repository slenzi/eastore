/*
 * Select all closure data needed to build a tree representation
 * This example selects node A, and all the required child data
 */
select
  n.parent_node_id, c.child_node_id, n.node_name, n.node_type
from
  eas_closure c
inner join
  eas_node n
on
  c.child_node_id = n.node_id
where
  c.parent_node_id = 1
order by
  c.depth, n.node_name

/*
 * Select tree path, from leaf node (J) to root node (A), i.e. J->A
 */
select
  p.parent_node_id, n.node_name, n.node_type, p.depth
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
 * Insert a new node.  First ? is the parent and second ? is the child.
 */
insert into eas_closure (link_id, parent_node_id, child_node_id, depth)
select
	eas_link_id_sequence.nextval, p.parent_node_id, c.child_node_id, (p.depth + c.depth + 1) as depth
from
	eas_closure p, eas_closure c
where
	p.child_node_id = ? and c.parent_node_id = ?

/*
 * select all rows to be deleted during a delete operation, see next query
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

/*
 * when deleting a node, this will insert the id of the node, plus all child nodes under it, into the prune table
 */
insert into eas_prune
select eas_prune_id_sequence.currval as prune_id, child_to_delete from (
  select distinct c.child_node_id as child_to_delete
  from eas_closure c
  inner join eas_node n
  on c.child_node_id = n.node_id 
  where c.parent_node_id = ?
)

/*
 * when deleting children of a node, this will add IDs of ALL children under the node, to the prune table.
 * this does not add the ID of the node itself because this query is onyl used when deleting children
 */
insert into eas_prune
select eas_prune_id_sequence.currval as prune_id, child_to_delete from (
  select distinct c.child_node_id as child_to_delete
  from eas_closure c
  inner join eas_node n
  on c.child_node_id = n.node_id 
  where c.parent_node_id = ?
  and c.depth > 0
)