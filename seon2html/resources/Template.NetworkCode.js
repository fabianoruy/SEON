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
@nodes], 

edges: [
@edges]
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