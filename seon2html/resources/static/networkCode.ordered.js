$(function(){ // on dom ready

$('#cy').cytoscape({
  style: cytoscape.stylesheet()
    .selector('node')
      .css({
        'content': 'data(name)',
		'width': 'data(dim)',
        'height': 'data(dim)',
		'background-color': 'data(color)',
		'font-size': 32,
        'text-wrap': 'wrap',
        'text-valign': 'center',
        'text-halign': 'center',
      })

	  .selector('edge')
      .css({
		'width': 'data(thickness)',
        'target-arrow-shape': 'triangle',
        'opacity': 0.5,
        'curve-style': 'bezier'
      })
	  
	  .selector('node:selected')
      .css({
		'border-width': '3px',
		'border-color': '#606060',
      })
	  
	  .selector('edge:selected')
      .css({
		'content': 'data(weight)',
		'font-size': 24,
		'text-outline-width': 5,
		'text-outline-color': 'white',
        'line-color': 'grey',
        'target-arrow-color': 'grey',
        'opacity': 1
      })
	  
      .selector('.faded')
      .css({
        'opacity': 0.25,
        'text-opacity': 0
      }),
  
  elements: {
nodes: [
  {data: {id:'0', name:'UFO', dim:192, level:1, color:'#99ffff'}},
  {data: {id:'3', name:'EO', dim:113, level:2, color:'#99ff99'}},
  {data: {id:'1', name:'SPO', dim:169, level:2, color:'#99ff99'}},
  {data: {id:'2', name:'SwO', dim:101, level:2, color:'#99ff99'}},
  {data: {id:'4', name:'RSRO', dim:112, level:3, color:'#ffff99'}},
  {data: {id:'5', name:'RDPO', dim:110, level:3, color:'#ffff99'}},
  {data: {id:'6', name:'DPO', dim:101, level:3, color:'#ffff99'}},
  {data: {id:'7', name:'CPO', dim:109, level:3, color:'#ffff99'}},
  {data: {id:'15', name:'DO7', dim:100, level:3, color:'#e1e1d0'}},
  {data: {id:'9', name:'DO1', dim:100, level:3, color:'#e1e1d0'}},
  {data: {id:'10', name:'DO2', dim:100, level:3, color:'#e1e1d0'}},
  {data: {id:'11', name:'DO3', dim:100, level:3, color:'#e1e1d0'}},
  {data: {id:'12', name:'DO4', dim:100, level:3, color:'#e1e1d0'}},
  {data: {id:'13', name:'DO5', dim:100, level:3, color:'#e1e1d0'}},
  {data: {id:'14', name:'DO6', dim:100, level:3, color:'#e1e1d0'}},
  {data: {id:'8', name:'TPO', dim:119, level:3, color:'#ffff99'}},
], 

edges: [
  {data: {id:'e0', thickness:20, weight:33, source:'1', target:'0'}},
  {data: {id:'e1', thickness:8, weight:13, source:'1', target:'3'}},
  {data: {id:'e2', thickness:1, weight:1, source:'2', target:'1'}},
  {data: {id:'e3', thickness:7, weight:10, source:'3', target:'0'}},
  {data: {id:'e4', thickness:2, weight:3, source:'4', target:'0'}},
  {data: {id:'e5', thickness:4, weight:6, source:'4', target:'1'}},
  {data: {id:'e6', thickness:6, weight:9, source:'4', target:'5'}},
  {data: {id:'e7', thickness:7, weight:10, source:'5', target:'1'}},
  {data: {id:'e8', thickness:5, weight:7, source:'5', target:'4'}},
  {data: {id:'e9', thickness:2, weight:2, source:'6', target:'1'}},
  {data: {id:'e10', thickness:6, weight:9, source:'7', target:'1'}},
  {data: {id:'e11', thickness:2, weight:2, source:'7', target:'4'}},
  {data: {id:'e12', thickness:2, weight:2, source:'7', target:'6'}},
  {data: {id:'e13', thickness:1, weight:1, source:'7', target:'8'}},
  {data: {id:'e14', thickness:9, weight:14, source:'8', target:'1'}},
  {data: {id:'e15', thickness:2, weight:2, source:'8', target:'7'}},
]
  },
  
  layout: {
	name: 'concentric',
	clockwise: false,
	minNodeSpacing: 40,
	concentric: function(){
	  return (4-this.data('level'))*10;
	},
  },
  
  ready: function(){
    window.cy = this;
  }
  
});

}); // on dom ready